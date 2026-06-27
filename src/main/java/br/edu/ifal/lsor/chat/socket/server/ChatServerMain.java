package br.edu.ifal.lsor.chat.socket.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import br.edu.ifal.lsor.chat.ChatMessage;

public class ChatServerMain {

    // Porta padrão onde o servidor ficará escutando
    public static final int SERVER_SOCKET_PORT = 8080;

    public static void main(String[] args) {
        // Inicializa o servidor e passa o método handleClient como o processador de cada conexão
        ChatServer server = new ChatServer(SERVER_SOCKET_PORT, ChatServerMain::handleClient);
        server.initServer();
    }

    /**
     * Método responsável por lidar com o fluxo de entrada de cada cliente conectado.
     * Ele roda em uma thread separada pelo ExecutorService do ChatServer.
     */
    private static void handleClient(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        System.out.println("[LOG] Novo cliente conectado: " + clientIp);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            
            // Lê continuamente as mensagens enviadas pelo cliente enquanto ele estiver conectado
            while ((line = reader.readLine()) != null) {
                
                // Transforma o texto JSON recebido de volta em um objeto ChatMessage
                ChatMessage message = ChatMessage.fromJson(line);
                
                // Exibe os detalhes da mensagem no terminal do servidor
                System.out.println("   [LOG] Mensagem recebida de " + clientIp + ":");
                System.out.println("   ├─ ID: " + message.getId());
                System.out.println("   ├─ Data: " + message.getCreatedAt());
                System.out.println("   └─ Texto: " + message.getMessage());
            }
        } catch (Exception exception) {
            System.err.println("[ERRO] Falha na comunicação com o cliente " + clientIp + ": " + exception.getMessage());
        } finally {
            try {
                // Garante que o socket do cliente seja fechado se ele desconectar ou der erro
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