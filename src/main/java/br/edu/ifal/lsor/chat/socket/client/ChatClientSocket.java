package br.edu.ifal.lsor.chat.socket.client;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import br.edu.ifal.lsor.chat.socket.ChatObjectInputFilters;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChatClientSocket implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger(ChatClientSocket.class);

  private final String host;
  private final int port;
  private final Consumer<ServerEvent> eventListener;
  private final Runnable disconnectListener;
  private final ConcurrentMap<UUID, CompletableFuture<ServerResponse>> pendingResponses =
      new ConcurrentHashMap<>();

  private Socket socket;
  private ObjectOutputStream output;
  private ObjectInputStream input;
  private Thread readerThread;
  private volatile boolean running;

  public ChatClientSocket(String host, int port) {
    this(host, port, event -> {});
  }

  public ChatClientSocket(String host, int port, Consumer<ServerEvent> eventListener) {
    this(host, port, eventListener, () -> {});
  }

  public ChatClientSocket(
      String host, int port, Consumer<ServerEvent> eventListener, Runnable disconnectListener) {
    this.host = host;
    this.port = port;
    this.eventListener = eventListener;
    this.disconnectListener = disconnectListener;
  }

  public void openSocket() {
    try {
      this.socket = new Socket(host, port);
      this.output = new ObjectOutputStream(socket.getOutputStream());
      this.output.flush();
      this.input = new ObjectInputStream(socket.getInputStream());
      ChatObjectInputFilters.applyProtocolFilter(input);
      this.running = true;
      this.readerThread = new Thread(this::readLoop, "chat-client-reader");
      this.readerThread.setDaemon(true);
      this.readerThread.start();

      LOGGER.info("Conectado com sucesso ao servidor {}:{}", host, port);

    } catch (Exception exception) {
      throw new IllegalStateException("Falha ao abrir socket.", exception);
    }
  }

  public Socket getSocket() {
    return socket;
  }

  public CompletableFuture<ServerResponse> send(ClientRequest request) {
    Objects.requireNonNull(request, "request");
    ensureOpen();

    CompletableFuture<ServerResponse> response = new CompletableFuture<>();
    pendingResponses.put(request.requestId(), response);
    try {
      synchronized (output) {
        output.writeObject(request);
        output.flush();
      }
      return response;
    } catch (Exception exception) {
      pendingResponses.remove(request.requestId());
      response.completeExceptionally(exception);
      throw new IllegalStateException("Falha ao enviar requisição.", exception);
    }
  }

  public CompletableFuture<ServerResponse> send(String action, Map<String, Serializable> payload) {
    return send(ClientRequest.of(action, payload));
  }

  private void ensureOpen() {
    if (socket == null || output == null) {
      throw new IllegalStateException("Abra o socket antes de enviar requisições.");
    }
    if (socket.isClosed() || !socket.isConnected()) {
      throw new IllegalStateException("Socket está fechado.");
    }
  }

  private void readLoop() {
    try {
      while (running) {
        Object object = input.readObject();
        if (object instanceof ServerResponse response) {
          CompletableFuture<ServerResponse> pending = pendingResponses.remove(response.requestId());
          if (pending != null) {
            pending.complete(response);
          }
        } else if (object instanceof ServerEvent event) {
          notifyEvent(event);
        }
      }
    } catch (Exception exception) {
      if (running) {
        completePending(exception);
        LOGGER.info("Conexao com o servidor encerrada.");
        disconnectListener.run();
      }
    } finally {
      running = false;
    }
  }

  private void completePending(Exception exception) {
    pendingResponses.forEach((requestId, response) -> response.completeExceptionally(exception));
    pendingResponses.clear();
  }

  private void notifyEvent(ServerEvent event) {
    try {
      eventListener.accept(event);
    } catch (RuntimeException exception) {
      LOGGER.warn("Listener de evento falhou para {}.", event.eventType());
    }
  }

  @Override
  public void close() throws Exception {
    running = false;
    if (this.socket != null && !this.socket.isClosed()) {
      this.socket.close();
      LOGGER.info("Conexão encerrada.");
    }
    if (readerThread != null) {
      readerThread.join(500);
    }
  }
}
