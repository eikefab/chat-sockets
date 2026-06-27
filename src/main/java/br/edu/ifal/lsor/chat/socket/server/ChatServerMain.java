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
        server.initServer();
    }

    private static void handleClient(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        System.out.println("[LOG] Novo cliente conectado: " + clientIp);

        // Abrimos o reader para ler e o writer para responder ao cliente
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            
            // Loop de escuta ativa
            while ((line = reader.readLine()) != null) {
                
                // Desserializa o JSON recebido
                ChatMessage message = ChatMessage.fromJson(line);
                
                // Logs do sistema no terminal do servidor
                System.out.println("   [LOG] Mensagem recebida de " + clientIp + ":");
                System.out.println("   |- ID: " + message.getId());
                System.out.println("   |- Data: " + message.getCreatedAt());
                System.out.println("   |- Texto: " + message.getMessage());

                // Task 16: Envia uma resposta de confirmacao direta para o socket do cliente
                writer.println("CONFIRMACAO: Mensagem recebida e processada pelo servidor.");
            }
        } catch (Exception exception) {
            System.err.println("[ERRO] Falha na comunicacao com o cliente " + clientIp + ": " + exception.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("[LOG] Cliente desconectado: " + clientIp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}