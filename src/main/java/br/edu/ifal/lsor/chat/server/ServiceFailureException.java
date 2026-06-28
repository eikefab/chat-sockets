package br.edu.ifal.lsor.chat.server;

final class ServiceFailureException extends Exception {

  private final String code;

  ServiceFailureException(String code, String message) {
    super(message);
    this.code = code;
  }

  String code() {
    return code;
  }
}
