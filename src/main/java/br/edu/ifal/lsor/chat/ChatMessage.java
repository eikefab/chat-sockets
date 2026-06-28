package br.edu.ifal.lsor.chat;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class ChatMessage implements Serializable {

  private static final long serialVersionUID = 1L;

  private final UUID id;
  private final String message;
  private final Instant createdAt;

  public ChatMessage(UUID id, String message, Instant createdAt) {
    this.id = id;
    this.message = message;
    this.createdAt = createdAt;
  }

  public ChatMessage(String message, Instant createdAt) {
    this(UUID.randomUUID(), message, createdAt);
  }

  public ChatMessage(String message) {
    this(message, Instant.now());
  }

  public UUID getId() {
    return id;
  }

  public String getMessage() {
    return message;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
