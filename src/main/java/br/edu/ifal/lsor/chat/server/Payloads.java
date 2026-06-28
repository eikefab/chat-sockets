package br.edu.ifal.lsor.chat.server;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

final class Payloads {

  private Payloads() {}

  static Serializable list(List<?> values) {
    return (Serializable) List.copyOf(values);
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
        "memberCount", group.members().size(),
        "isMember", group.members().contains(username));
  }
}
