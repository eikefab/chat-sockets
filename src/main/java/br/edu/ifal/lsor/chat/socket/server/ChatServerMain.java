package br.edu.ifal.lsor.chat.socket.server;

public final class ChatServerMain {

  public static final int SERVER_SOCKET_PORT = 3000;

  public static void main(String[] args) {
    final ChatServer server = new ChatServer(SERVER_SOCKET_PORT, (socket) -> {
      System.out.println("Hello World!");
    });

    server.initServer();
  }

}
