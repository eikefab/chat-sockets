package br.edu.ifal.lsor.chat.client;

import br.edu.ifal.lsor.chat.protocol.ServerResponse;

public final class ChatResponseException extends RuntimeException {

  private final ServerResponse response;

  ChatResponseException(ServerResponse response) {
    super(response.message());
    this.response = response;
  }

  public ServerResponse response() {
    return response;
  }
}
