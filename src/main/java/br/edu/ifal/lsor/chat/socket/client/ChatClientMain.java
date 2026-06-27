package br.edu.ifal.lsor.chat.socket.client;

import br.edu.ifal.lsor.chat.ChatMessage;
import br.edu.ifal.lsor.chat.socket.server.ChatServerMain;

public class ChatClientMain {

    public static void main(String[] args) {
        // Inicializa o cliente apontando para o localhost e a porta do servidor
        System.out.println("[SISTEMA] Iniciando cliente de teste...");
        
        try (ChatClientSocket client = new ChatClientSocket("127.0.0.1", ChatServerMain.SERVER_SOCKET_PORT)) {
            
            // Tenta abrir a conexao
            client.openSocket();

            // Task 23: Testar conexao e envio de mensagem
            ChatMessage olaMensagem = new ChatMessage("Ola grupo! Testando a conexao.");
            System.out.println("[CLIENTE] Enviando mensagem...");
            
            client.write(reader -> {
                try {
                    String resposta = reader.readLine();
                    System.out.println("[SERVIDOR RESPONDEU]: " + resposta);
                } catch (Exception e) {
                    System.err.println("[ERRO] Falha ao ler resposta: " + e.getMessage());
                }
            }, olaMensagem.toJson());

            // Pausa de 2 segundos para podermos ver o log no console
            Thread.sleep(2000);

            // Task 24 e 25: Testar encerramento enviando o comando /sair
            ChatMessage sairMensagem = new ChatMessage("/sair");
            System.out.println("[CLIENTE] Enviando comando de saida...");
            
            client.write(reader -> {
                try {
                    String resposta = reader.readLine();
                    System.out.println("[SERVIDOR RESPONDEU]: " + resposta);
                } catch (Exception e) {
                    System.err.println("[ERRO] Falha ao ler resposta de saida: " + e.getMessage());
                }
            }, sairMensagem.toJson());

        } catch (Exception e) {
            System.err.println("[ERRO CRITICO] Falha na execucao do cliente: " + e.getMessage());
        }
        
        System.out.println("[SISTEMA] Cliente de teste finalizado.");
    }
}