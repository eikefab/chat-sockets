package br.edu.ifal.lsor.chat.socket.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChatServer {

  private static final Logger LOGGER = LogManager.getLogger(ChatServer.class);

  private final int port;
  private final ExecutorService pool;
  private final Consumer<Socket> handler;
  private final CountDownLatch started = new CountDownLatch(1);

  private ServerSocket serverSocket;
  private volatile boolean isRunning;

  public ChatServer(int port, Consumer<Socket> handler) {
    this(
        port,
        handler,
        Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors())));
  }

  public ChatServer(int port, Consumer<Socket> handler, ExecutorService pool) {
    this.port = port;
    this.pool = Objects.requireNonNull(pool, "pool");
    this.handler = Objects.requireNonNull(handler, "handler");
  }

  public void initServer() {
    this.isRunning = true;
    try {
      this.serverSocket = new ServerSocket(port);
      started.countDown();
      LOGGER.info("[LOG] Servidor iniciado na porta {}", port);

      while (isRunning) {
        final Socket socket = serverSocket.accept();
        pool.submit(() -> handler.accept(socket));
      }
    } catch (Exception exception) {
      started.countDown();
      if (isRunning) {
        LOGGER.error("[ERRO] Falha no servidor: {}", exception.getMessage());
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
      LOGGER.info("[LOG] Servidor encerrado com segurança.");
    } catch (Exception e) {
      LOGGER.error("[ERRO] Falha ao encerrar o servidor: {}", e.getMessage());
    }
  }

  public int getBoundPort() {
    if (serverSocket == null) {
      return port;
    }
    return serverSocket.getLocalPort();
  }

  public boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
    return started.await(timeout, unit);
  }
}
