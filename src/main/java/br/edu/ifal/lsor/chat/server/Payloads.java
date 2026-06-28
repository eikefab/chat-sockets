package br.edu.ifal.lsor.chat.server;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class Payloads {

  private Payloads() {}

  static Serializable list(List<?> values) {
    return (Serializable) List.copyOf(values);
  }

  static Map<String, Serializable> login(UserRecord user) {
    return Map.of(
        "memberId", user.memberId(),
        "username", user.username(),
        "displayName", user.displayName());
  }

  static Map<String, Serializable> group(GroupRecord group) {
    return Map.of(
        "groupId", group.groupId(),
        "groupCode", group.groupCode(),
        "displayName", group.displayName(),
        "ownerUsername", group.ownerUsername());
  }

  static Map<String, Serializable> user(UserRecord user, boolean online) {
    return Map.of(
        "memberId", user.memberId(),
        "username", user.username(),
        "displayName", user.displayName(),
        "online", online);
  }

  static Map<String, Serializable> groupSummary(GroupRecord group, String username) {
    return Map.of(
        "groupId", group.groupId(),
        "groupCode", group.groupCode(),
        "displayName", group.displayName(),
        "ownerUsername", group.ownerUsername(),
        "memberCount", group.memberCount(),
        "isMember", group.hasMember(username));
  }

  static Map<String, Serializable> messageAccepted(
      UUID messageId, Instant createdAt, boolean deliveredToOnline) {
    return Map.of(
        "messageId", messageId,
        "createdAt", createdAt,
        "deliveredToOnline", deliveredToOnline);
  }

  static Map<String, Serializable> messageAcceptedWithOnlineRecipients(
      UUID messageId, Instant createdAt, int onlineRecipients) {
    return Map.of(
        "messageId", messageId,
        "createdAt", createdAt,
        "onlineRecipients", onlineRecipients);
  }

  static Map<String, Serializable> directMessageEvent(
      UUID messageId, String fromUsername, String text, Instant createdAt) {
    return Map.of(
        "messageId", messageId,
        "fromUsername", fromUsername,
        "text", text,
        "createdAt", createdAt);
  }

  static Map<String, Serializable> groupMessageEvent(
      UUID messageId,
      String groupCode,
      String groupDisplayName,
      String authorUsername,
      String text,
      Instant createdAt) {
    return Map.of(
        "messageId", messageId,
        "groupCode", groupCode,
        "groupDisplayName", groupDisplayName,
        "authorUsername", authorUsername,
        "text", text,
        "createdAt", createdAt);
  }

  static Map<String, Serializable> groupJoinLeaveEvent(
      String groupCode, String username, String displayName) {
    return Map.of(
        "groupCode", groupCode,
        "username", username,
        "displayName", displayName);
  }

  static Map<String, Serializable> historyResult(
      String scope, String target, List<Map<String, Serializable>> messages) {
    return Map.of("scope", scope, "target", target, "messages", list(messages));
  }
}
