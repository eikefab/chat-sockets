package br.edu.ifal.lsor.chat.server;

final class InvalidPayloadException extends Exception {

  InvalidPayloadException(String message) {
    super(message);
  }
}
