package br.edu.ifal.lsor.chat.socket.server;

import br.edu.ifal.lsor.chat.socket.ChatObjectStreams;
import java.io.ObjectOutputStream;

final class ClientConnection {

  private final ObjectOutputStream output;

  ClientConnection(ObjectOutputStream output) {
    this.output = output;
  }

  synchronized void send(Object object) {
    try {
      ChatObjectStreams.writeAndReset(output, object);
    } catch (Exception exception) {
      throw new IllegalStateException("Falha ao enviar objeto ao cliente.", exception);
    }
  }
}
