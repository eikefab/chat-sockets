package br.edu.ifal.lsor.chat.socket.client;

import br.edu.ifal.lsor.chat.ChatMessage;
import br.edu.ifal.lsor.chat.socket.server.ChatServerMain;

public class ChatClientMain {

    public static void main(String[] args) {
        System.out.println("[SISTEMA] Iniciando cliente de teste...");
        
        try (ChatClientSocket client = new ChatClientSocket("127.0.0.1", ChatServerMain.SERVER_SOCKET_PORT)) {
            client.openSocket();

            ChatMessage olaMensagem = new ChatMessage("Ola grupo! Testando a conexao.");
            System.out.println("[CLIENTE] Enviando mensagem...");
            System.out.println("[SERVIDOR RESPONDEU]: " + client.send(olaMensagem));

            Thread.sleep(2000);

            ChatMessage sairMensagem = new ChatMessage("/sair");
            System.out.println("[CLIENTE] Enviando comando de saida...");
            System.out.println("[SERVIDOR RESPONDEU]: " + client.send(sairMensagem));

        } catch (Exception e) {
            System.err.println("[ERRO CRITICO] Falha na execucao do cliente: " + e.getMessage());
        }
        
        System.out.println("[SISTEMA] Cliente de teste finalizado.");
    }
}
