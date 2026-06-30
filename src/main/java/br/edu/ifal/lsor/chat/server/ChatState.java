package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.Codes;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class ChatState {

  private final Map<String, UserRecord> users = new HashMap<>();
  private final Map<String, ChatSession> onlineSessions = new HashMap<>();
  private final Map<String, GroupRecord> groups = new HashMap<>();
  private final Map<String, Deque<MessageRecord>> pendingDirectMessages = new HashMap<>();
  private final MessageHistory directHistory = new MessageHistory();

  boolean isOnline(String username) {
    return onlineSessions.containsKey(username);
  }

  Set<String> onlineUsernames() {
    return new LinkedHashSet<>(onlineSessions.keySet());
  }

  Set<String> onlineMembers(Set<String> usernames) {
    Set<String> targets = new LinkedHashSet<>();
    for (String username : usernames) {
      if (isOnline(username)) {
        targets.add(username);
      }
    }
    return targets;
  }

  List<UserRecord> usersSortedByUsername() {
    return users.values().stream().sorted(Comparator.comparing(UserRecord::username)).toList();
  }

  UserRecord findUser(String username) {
    return users.get(username);
  }

  UserRecord saveUser(String username, String displayName) {
    return users.compute(
        username,
        (key, existing) ->
            existing == null
                ? new UserRecord(UUID.randomUUID(), username, displayName)
                : new UserRecord(existing.memberId(), username, displayName));
  }

  void connect(UserRecord user, ChatSession session) {
    onlineSessions.put(user.username(), session);
  }

  UserRecord disconnect(ChatSession session) {
    if (!session.isAuthenticated()) {
      return null;
    }
    UserRecord user = findUser(session.username());
    onlineSessions.remove(session.username());
    session.clear();
    return user;
  }

  List<GroupRecord> groupsSortedByCode() {
    return groups.values().stream().sorted(Comparator.comparing(GroupRecord::groupCode)).toList();
  }

  GroupRecord findGroup(String groupCode) {
    return groups.get(groupCode);
  }

  GroupRecord requireGroup(String groupCode) throws ServiceFailureException {
    GroupRecord group = findGroup(groupCode);
    if (group == null) {
      throw new ServiceFailureException(Codes.GROUP_NOT_FOUND, "Grupo não encontrado.");
    }
    return group;
  }

  GroupRecord createGroup(String groupCode, String displayName, String ownerUsername) {
    GroupRecord group = new GroupRecord(UUID.randomUUID(), groupCode, displayName, ownerUsername);
    group.addMember(ownerUsername);
    groups.put(groupCode, group);
    return group;
  }

  void removeGroup(GroupRecord group) {
    groups.remove(group.groupCode());
  }

  void addDirectMessage(MessageRecord message) {
    directHistory.add(message);
  }

  void enqueuePendingDirectMessage(String username, MessageRecord message) {
    pendingDirectMessages.computeIfAbsent(username, key -> new ArrayDeque<>()).addLast(message);
  }

  List<MessageRecord> drainPendingDirectMessages(String username) {
    Deque<MessageRecord> messages = pendingDirectMessages.remove(username);
    return messages == null ? List.of() : List.copyOf(messages);
  }

  List<MessageRecord> directHistoryBetween(String firstUsername, String secondUsername) {
    return directHistory.between(firstUsername, secondUsername);
  }
}
