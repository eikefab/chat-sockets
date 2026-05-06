package br.edu.ifal.lsor.chat.socket.client;

import br.edu.ifal.lsor.chat.socket.server.ChatServerMain;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

public class ChatClientSocket implements AutoCloseable {

  private static final String HOST = "127.0.0.1";

  private Socket socket;
  private PrintWriter writer;
  private BufferedReader reader;

  public void openSocket() {
    try {
      this.socket = new Socket(HOST, ChatServerMain.SERVER_SOCKET_PORT);

      this.writer = new PrintWriter(socket.getOutputStream());
      this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  public Socket getSocket() {
    return socket;
  }

  public void write(Consumer<BufferedReader> response, Object... lines) {
    Objects.requireNonNull(socket);
    Objects.requireNonNull(writer);
    Objects.requireNonNull(reader);

    if (this.socket.isClosed() || !this.socket.isConnected()) {
      throw new IllegalStateException("Socket está fechado!");
    }

    for (Object line : lines) {
      final String data = line.toString();

      writer.println(data);
    }

    writer.flush();

    response.accept(reader);
  }

  @Override
  public void close() throws Exception {
    this.socket.close();
  }
}
