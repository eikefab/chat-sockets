package br.edu.ifal.lsor.chat.socket.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ChatServer {

  private final int port;
  private final ExecutorService pool;
  private final Consumer<Socket> handler;

  public ChatServer(int port, Consumer<Socket> handler) {
    this.port = port;
    this.pool = Executors.newCachedThreadPool();
    this.handler = handler;
  }

  public void initServer() {
    try (ServerSocket socketServer = new ServerSocket(port)) {
      System.out.format("Servidor iniciado na porta %d", port)
              .println();

      while (true) {
        final Socket socket = socketServer.accept();

        pool.submit(() -> handler.accept(socket));
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

}
