package br.edu.ifal.lsor.chat.server;

enum MessageScope {
  DIRECT,
  GROUP;

  static MessageScope parse(String value) throws InvalidPayloadException {
    try {
      return MessageScope.valueOf(value);
    } catch (IllegalArgumentException exception) {
      throw new InvalidPayloadException("Scope inválido.");
    }
  }
}
