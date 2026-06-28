package br.edu.ifal.lsor.chat.server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class GroupRecord {

  private final UUID groupId;
  private final String groupCode;
  private final String ownerUsername;
  private final Set<String> members = new LinkedHashSet<>();
  private final List<MessageRecord> history = new ArrayList<>();
  private String displayName;

  GroupRecord(UUID groupId, String groupCode, String displayName, String ownerUsername) {
    this.groupId = groupId;
    this.groupCode = groupCode;
    this.displayName = displayName;
    this.ownerUsername = ownerUsername;
  }

  UUID groupId() {
    return groupId;
  }

  String groupCode() {
    return groupCode;
  }

  String displayName() {
    return displayName;
  }

  void rename(String displayName) {
    this.displayName = displayName;
  }

  String ownerUsername() {
    return ownerUsername;
  }

  Set<String> memberUsernames() {
    return Set.copyOf(members);
  }

  int memberCount() {
    return members.size();
  }

  boolean hasMember(String username) {
    return members.contains(username);
  }

  boolean addMember(String username) {
    return members.add(username);
  }

  boolean removeMember(String username) {
    return members.remove(username);
  }

  void addMessage(MessageRecord message) {
    history.add(message);
  }

  List<MessageRecord> historySnapshot() {
    return List.copyOf(history);
  }
}
