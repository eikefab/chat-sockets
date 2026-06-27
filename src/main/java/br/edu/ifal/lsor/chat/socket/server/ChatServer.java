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
  
  private ServerSocket serverSocket; // Guardamos a referencia para poder fechar depois
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

      // Loop principal: O servidor continua aceitando clientes enquanto estiver rodando
      while (isRunning) {
        final Socket socket = serverSocket.accept();
        pool.submit(() -> handler.accept(socket));
      }
    } catch (Exception exception) {
      // Se o servidor for parado intencionalmente, um erro de interrupcao e normal aqui,
      // entao so exibimos erro se ele ainda deveria estar rodando.
      if (isRunning) {
        exception.printStackTrace();
      }
    }
  }

  // Task 20: Metodo para encerrar o servidor com seguranca
  public void stopServer() {
    this.isRunning = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close(); // Libera a porta de rede do sistema operacional
      }
      pool.shutdown(); // Avisa ao gerenciador de threads para parar de aceitar novas tarefas
      System.out.println("[LOG] Servidor encerrado com seguranca.");
    } catch (Exception e) {
      System.err.println("[ERRO] Falha ao encerrar o servidor: " + e.getMessage());
    }
  }
}