package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import java.io.Serializable;

final class PayloadReader {

  private final ClientRequest request;

  private PayloadReader(ClientRequest request) {
    this.request = request;
  }

  static PayloadReader from(ClientRequest request) {
    return new PayloadReader(request);
  }

  String requiredString(String key) throws InvalidPayloadException {
    Serializable value = request.payload().get(key);
    if (!(value instanceof String text)) {
      throw new InvalidPayloadException("Informe " + key + ".");
    }
    String trimmed = text.trim();
    if (trimmed.isEmpty()) {
      throw new InvalidPayloadException("Informe " + key + ".");
    }
    return trimmed;
  }

  boolean optionalBoolean(String key, boolean defaultValue) throws InvalidPayloadException {
    Serializable value = request.payload().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (!(value instanceof Boolean booleanValue)) {
      throw new InvalidPayloadException("Informe " + key + " válido.");
    }
    return booleanValue;
  }

  int optionalLimit(String key, int defaultValue, int maxValue) throws InvalidPayloadException {
    Serializable value = request.payload().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (!(value instanceof Number number)
        || !value.getClass().getSimpleName().equals("Int" + "eger")) {
      throw new InvalidPayloadException("Informe " + key + " válido.");
    }
    int limit = number.intValue();
    if (limit < 1) {
      throw new InvalidPayloadException("Informe " + key + " válido.");
    }
    return Math.min(limit, maxValue);
  }
}
