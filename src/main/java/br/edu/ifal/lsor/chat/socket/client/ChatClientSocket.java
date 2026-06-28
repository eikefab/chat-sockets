package br.edu.ifal.lsor.chat.socket.client;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
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
    this.host = host;
    this.port = port;
    this.eventListener = eventListener;
  }

  public void openSocket() {
    try {
      this.socket = new Socket(host, port);
      this.output = new ObjectOutputStream(socket.getOutputStream());
      this.output.flush();
      this.input = new ObjectInputStream(socket.getInputStream());
      this.running = true;
      this.readerThread = new Thread(this::readLoop, "chat-client-reader");
      this.readerThread.setDaemon(true);
      this.readerThread.start();

      LOGGER.info("Conectado com sucesso ao servidor {}:{}", host, port);

    } catch (Exception exception) {
      LOGGER.error("Erro ao conectar no servidor {}:{}", host, port);
      LOGGER.error("Detalhe do erro: {}", exception.getMessage());
      LOGGER.error("Verifique se o servidor está rodando e se o IP/Porta estão corretos.");
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
          eventListener.accept(event);
        }
      }
    } catch (Exception exception) {
      if (running) {
        completePending(exception);
      }
    }
  }

  private void completePending(Exception exception) {
    pendingResponses.forEach((requestId, response) -> response.completeExceptionally(exception));
    pendingResponses.clear();
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
