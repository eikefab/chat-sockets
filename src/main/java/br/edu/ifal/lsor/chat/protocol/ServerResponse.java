package br.edu.ifal.lsor.chat.protocol;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ServerResponse(
    String protocolVersion,
    UUID requestId,
    String status,
    String code,
    String message,
    Map<String, Serializable> payload,
    Instant respondedAt)
    implements Serializable {

  private static final long serialVersionUID = 1L;

  public ServerResponse {
    payload = payload == null ? Map.of() : Map.copyOf(payload);
  }

  public static ServerResponse ok(
      UUID requestId, String code, String message, Map<String, Serializable> payload) {
    return new ServerResponse(
        Protocol.VERSION, requestId, "OK", code, message, payload, Instant.now());
  }

  public static ServerResponse error(UUID requestId, String code, String message) {
    return new ServerResponse(
        Protocol.VERSION, requestId, "ERROR", code, message, Map.of(), Instant.now());
  }

  public boolean isOk() {
    return "OK".equals(status);
  }
}
