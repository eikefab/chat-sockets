package br.edu.ifal.lsor.chat.server;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

record MessageRecord(
    UUID messageId,
    String fromUsername,
    String toUsername,
    String groupCode,
    String text,
    Instant createdAt) {

  static MessageRecord direct(
      UUID messageId, String fromUsername, String toUsername, String text, Instant createdAt) {
    return new MessageRecord(messageId, fromUsername, toUsername, null, text, createdAt);
  }

  static MessageRecord group(
      UUID messageId, String authorUsername, String groupCode, String text, Instant createdAt) {
    return new MessageRecord(messageId, authorUsername, null, groupCode, text, createdAt);
  }

  boolean isDirectBetween(String firstUsername, String secondUsername) {
    return groupCode == null
        && ((fromUsername.equals(firstUsername) && toUsername.equals(secondUsername))
            || (fromUsername.equals(secondUsername) && toUsername.equals(firstUsername)));
  }

  Map<String, Serializable> toPayload() {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("messageId", messageId);
    payload.put("fromUsername", fromUsername);
    payload.put("text", text);
    payload.put("createdAt", createdAt);
    if (toUsername != null) {
      payload.put("toUsername", toUsername);
    }
    if (groupCode != null) {
      payload.put("groupCode", groupCode);
    }
    return Map.copyOf(payload);
  }
}
