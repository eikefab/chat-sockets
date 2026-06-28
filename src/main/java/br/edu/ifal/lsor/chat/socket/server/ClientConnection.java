package br.edu.ifal.lsor.chat.socket.server;

import java.io.ObjectOutputStream;

final class ClientConnection {

  private final ObjectOutputStream output;

  ClientConnection(ObjectOutputStream output) {
    this.output = output;
  }

  synchronized void send(Object object) {
    try {
      output.writeObject(object);
      output.flush();
    } catch (Exception exception) {
      throw new IllegalStateException("Falha ao enviar objeto ao cliente.", exception);
    }
  }
}
