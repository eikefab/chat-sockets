package br.edu.ifal.lsor.chat.server;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

record MessageRecord(
    UUID messageId,
    String fromUsername,
    ConversationTarget target,
    String text,
    Instant createdAt) {

  static MessageRecord direct(
      UUID messageId, String fromUsername, String toUsername, String text, Instant createdAt) {
    return new MessageRecord(
        messageId, fromUsername, ConversationTarget.direct(toUsername), text, createdAt);
  }

  static MessageRecord group(
      UUID messageId, String authorUsername, String groupCode, String text, Instant createdAt) {
    return new MessageRecord(
        messageId, authorUsername, ConversationTarget.group(groupCode), text, createdAt);
  }

  boolean isDirectBetween(String firstUsername, String secondUsername) {
    return target.isDirectBetween(firstUsername, secondUsername)
        && (fromUsername.equals(firstUsername) || fromUsername.equals(secondUsername));
  }

  Map<String, Serializable> toPayload() {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("messageId", messageId);
    payload.put("fromUsername", fromUsername);
    payload.put("text", text);
    payload.put("createdAt", createdAt);
    if (target.scope() == MessageScope.DIRECT) {
      payload.put("toUsername", target.value());
    } else {
      payload.put("groupCode", target.value());
    }
    return Map.copyOf(payload);
  }
}
