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
  
  private ServerSocket serverSocket;
  private boolean isRunning;

  public ChatServer(int port, Consumer<Socket> handler) {
    this.port = port;
    this.pool = Executors.newCachedThreadPool();
    this.handler = handler;
  }

  public void initServer() {
    this.isRunning = true;
    try {
      this.serverSocket = new ServerSocket(port);
      System.out.println("[LOG] Servidor iniciado na porta " + port);

      while (isRunning) {
        final Socket socket = serverSocket.accept();
        pool.submit(() -> handler.accept(socket));
      }
    } catch (Exception exception) {
      if (isRunning) {
        exception.printStackTrace();
      }
    }
  }

  public void stopServer() {
    this.isRunning = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
      pool.shutdown();
      System.out.println("[LOG] Servidor encerrado com seguranca.");
    } catch (Exception e) {
      System.err.println("[ERRO] Falha ao encerrar o servidor: " + e.getMessage());
    }
  }
}
