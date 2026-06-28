package br.edu.ifal.lsor.chat.socket.server;

import br.edu.ifal.lsor.chat.server.InMemoryChatService;

/**
 * Entrypoint legado/de teste. Prefira {@link br.edu.ifal.lsor.chat.ChatApplicationMain} com {@code
 * --server}.
 */
@Deprecated(forRemoval = false)
public class ChatServerMain {

  public static final int SERVER_SOCKET_PORT = 8080;

  public static void main(String[] args) {
    ChatProtocolSocketHandler handler = new ChatProtocolSocketHandler(new InMemoryChatService());
    ChatServer server =
        new ChatServer(
            "0.0.0.0", SERVER_SOCKET_PORT, ChatServer.DEFAULT_MAX_CLIENTS, handler::handle);
    Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer));

    server.initServer();
  }
}
