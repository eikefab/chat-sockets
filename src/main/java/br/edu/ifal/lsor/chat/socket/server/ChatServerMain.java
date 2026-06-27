package br.edu.ifal.lsor.chat.socket.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import br.edu.ifal.lsor.chat.ChatMessage;

public class ChatServerMain {

    public static final int SERVER_SOCKET_PORT = 8080;

    public static void main(String[] args) {
        ChatServer server = new ChatServer(SERVER_SOCKET_PORT, ChatServerMain::handleClient);
        
        // Adiciona um gatilho ("hook") no sistema operacional.
        // Se voce apertar Ctrl+C no terminal, ele chama o stopServer() antes de morrer.
        Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer));
        
        server.initServer();
    }

    private static void handleClient(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        System.out.println("[LOG] Novo cliente conectado: " + clientIp);

        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                ChatMessage message = ChatMessage.fromJson(line);
                
                // Task 6 e 22: Identifica o comando de saida
                if (message.getMessage().trim().equalsIgnoreCase("/sair")) {
                    System.out.println("[LOG] Cliente " + clientIp + " solicitou desconexao.");
                    writer.println("CONFIRMACAO: Voce foi desconectado do servidor.");
                    break; // Quebra o loop, enviando o fluxo direto para o finally (onde o socket fecha)
                }

                System.out.println("[LOG] Mensagem recebida de " + clientIp + ":");
                System.out.println("   |- ID: " + message.getId());
                System.out.println("   |- Texto: " + message.getMessage());

                writer.println("CONFIRMACAO: Mensagem recebida pelo servidor.");
            }
        } catch (Exception exception) {
            // Task 22: Trata quando o cliente cai sem avisar (ex: fechou o terminal abruptamente)
            System.err.println("[AVISO] Conexao perdida com o cliente " + clientIp + ".");
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("[LOG] Cliente desconectado e socket fechado: " + clientIp);
                }
            } catch (Exception e) {
                System.err.println("[ERRO] Falha ao fechar socket do cliente.");
            }
        }
    }
}