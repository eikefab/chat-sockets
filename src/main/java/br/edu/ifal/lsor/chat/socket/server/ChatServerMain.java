package br.edu.ifal.lsor.chat.socket.server;

import br.edu.ifal.lsor.chat.server.InMemoryChatService;

public class ChatServerMain {

  public static final int SERVER_SOCKET_PORT = 8080;

  public static void main(String[] args) {
    ChatProtocolSocketHandler handler = new ChatProtocolSocketHandler(new InMemoryChatService());
    ChatServer server = new ChatServer(SERVER_SOCKET_PORT, handler::handle);
    Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer));

    server.initServer();
  }
}
