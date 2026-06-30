package br.edu.ifal.lsor.chat.client;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

public record ChatMessage(
    ConversationKind kind,
    String conversationId,
    String authorUsername,
    String text,
    Instant createdAt,
    boolean ownMessage) {

  public static ChatMessage fromHistory(
      ConversationKind kind,
      String conversationId,
      String ownUsername,
      Map<String, Serializable> payload) {
    String author = (String) payload.get("fromUsername");
    String text = (String) payload.get("text");
    Instant createdAt = (Instant) payload.get("createdAt");
    return new ChatMessage(
        kind,
        conversationId,
        author,
        text,
        createdAt,
        ownUsername != null && ownUsername.equals(author));
  }
}
