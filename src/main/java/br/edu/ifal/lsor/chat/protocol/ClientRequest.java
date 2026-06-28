package br.edu.ifal.lsor.chat.protocol;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ClientRequest(
    String protocolVersion,
    UUID requestId,
    String action,
    Instant sentAt,
    Map<String, Serializable> payload)
    implements Serializable {

  private static final long serialVersionUID = 1L;

  public ClientRequest {
    payload = payload == null ? Map.of() : Map.copyOf(payload);
  }

  public static ClientRequest of(String action, Map<String, Serializable> payload) {
    return new ClientRequest(Protocol.VERSION, UUID.randomUUID(), action, Instant.now(), payload);
  }
}
