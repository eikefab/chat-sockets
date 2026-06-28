package br.edu.ifal.lsor.chat.socket.server;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import br.edu.ifal.lsor.chat.ChatMessage;

public class ChatServerMain {

    public static final int SERVER_SOCKET_PORT = 8080;

    public static void main(String[] args) {
        ChatServer server = new ChatServer(SERVER_SOCKET_PORT, ChatServerMain::handleClient);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer));
        
        server.initServer();
    }

    private static void handleClient(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        System.out.println("[LOG] Novo cliente conectado: " + clientIp);

        try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            output.flush();

            try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                while (true) {
                    ChatMessage message = (ChatMessage) input.readObject();

                    if (message.getMessage().trim().equalsIgnoreCase("/sair")) {
                        System.out.println("[LOG] Cliente " + clientIp + " solicitou desconexao.");
                        output.writeObject("CONFIRMACAO: Voce foi desconectado do servidor.");
                        output.flush();
                        break;
                    }

                    System.out.println("[LOG] Mensagem recebida de " + clientIp + ":");
                    System.out.println("   |- ID: " + message.getId());
                    System.out.println("   |- Texto: " + message.getMessage());

                    output.writeObject("CONFIRMACAO: Mensagem recebida pelo servidor.");
                    output.flush();
                }
            }
        } catch (EOFException exception) {
            System.err.println("[AVISO] Conexao encerrada pelo cliente " + clientIp + ".");
        } catch (Exception exception) {
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
