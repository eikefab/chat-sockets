package br.edu.ifal.lsor.chat.protocol;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ServerEvent(
    String protocolVersion,
    UUID eventId,
    String eventType,
    Instant emittedAt,
    Map<String, Serializable> payload)
    implements Serializable {

  private static final long serialVersionUID = 1L;

  public ServerEvent {
    payload = payload == null ? Map.of() : Map.copyOf(payload);
  }

  public static ServerEvent of(String eventType, Map<String, Serializable> payload) {
    return new ServerEvent(Protocol.VERSION, UUID.randomUUID(), eventType, Instant.now(), payload);
  }
}
