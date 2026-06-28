package br.edu.ifal.lsor.chat.socket.server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChatServer {

  private static final Logger LOGGER = LogManager.getLogger(ChatServer.class);

  private final String host;
  private final int port;
  private final int maxClients;
  private final ExecutorService pool;
  private final Consumer<Socket> handler;
  private final CountDownLatch started = new CountDownLatch(1);
  private final Semaphore availableConnections;
  private final Set<Socket> activeSockets = ConcurrentHashMap.newKeySet();

  private ServerSocket serverSocket;
  private volatile boolean isRunning;

  static final int DEFAULT_MAX_CLIENTS = 50;

  public ChatServer(int port, Consumer<Socket> handler) {
    this("0.0.0.0", port, DEFAULT_MAX_CLIENTS, handler, defaultPool());
  }

  public ChatServer(String host, int port, int maxClients, Consumer<Socket> handler) {
    this(host, port, maxClients, handler, Executors.newFixedThreadPool(maxClients));
  }

  public ChatServer(
      String host, int port, int maxClients, Consumer<Socket> handler, ExecutorService pool) {
    if (maxClients <= 0) {
      throw new IllegalArgumentException("maxClients deve ser maior que zero.");
    }
    this.host = Objects.requireNonNull(host, "host");
    this.port = port;
    this.maxClients = maxClients;
    this.pool = Objects.requireNonNull(pool, "pool");
    this.handler = Objects.requireNonNull(handler, "handler");
    this.availableConnections = new Semaphore(maxClients);
  }

  private static ExecutorService defaultPool() {
    return Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
  }

  public void initServer() {
    this.isRunning = true;
    try {
      this.serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));
      started.countDown();
      LOGGER.info(
          "Servidor iniciado em {}:{} com limite de {} clientes.",
          host,
          getBoundPort(),
          maxClients);

      while (isRunning) {
        final Socket socket = serverSocket.accept();
        if (!availableConnections.tryAcquire()) {
          rejectConnection(socket);
          continue;
        }
        activeSockets.add(socket);
        submitConnection(socket);
      }
    } catch (Exception exception) {
      started.countDown();
      if (isRunning) {
        LOGGER.error("Falha no servidor: {}", exception.getMessage());
      }
    }
  }

  public void stopServer() {
    this.isRunning = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
      closeActiveSockets();
      pool.shutdown();
      LOGGER.info("Servidor encerrado com segurança.");
    } catch (Exception e) {
      LOGGER.error("Falha ao encerrar o servidor: {}", e.getMessage());
    }
  }

  private void submitConnection(Socket socket) {
    try {
      pool.submit(
          () -> {
            try {
              handler.accept(socket);
            } finally {
              activeSockets.remove(socket);
              availableConnections.release();
            }
          });
    } catch (RejectedExecutionException exception) {
      activeSockets.remove(socket);
      availableConnections.release();
      rejectConnection(socket);
    }
  }

  private void closeActiveSockets() {
    for (Socket socket : activeSockets) {
      try {
        socket.close();
      } catch (Exception exception) {
        LOGGER.warn("Falha ao fechar conexao ativa: {}", exception.getMessage());
      }
    }
    activeSockets.clear();
  }

  private void rejectConnection(Socket socket) {
    try {
      LOGGER.warn("Limite de {} clientes atingido. Conexao recusada.", maxClients);
      socket.close();
    } catch (Exception exception) {
      LOGGER.warn("Falha ao recusar conexao excedente: {}", exception.getMessage());
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

  int maxClients() {
    return maxClients;
  }
}
