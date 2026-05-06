package br.edu.ifal.lsor.chat.group;

import br.edu.ifal.lsor.chat.ChatHistory;
import br.edu.ifal.lsor.chat.member.ChatMember;

import java.util.*;
import java.util.stream.Collectors;

public abstract class ChatGroup {

  private final UUID id;
  private final String code;
  private final String displayName;
  private final ChatMember owner;
  private final Set<ChatMember> members;
  private final ChatHistory<ChatGroupMessage> history;

  public ChatGroup(UUID id, String code, String displayName, ChatMember owner, Set<ChatMember> members, ChatHistory<ChatGroupMessage> history) {
    this.id = id;
    this.code = code;
    this.displayName = displayName;
    this.owner = owner;
    this.members = members;
    this.history = history;
  }

  public ChatGroup(String code, String displayName, ChatMember owner, Set<ChatMember> members, ChatHistory<ChatGroupMessage> history) {
    this(UUID.randomUUID(), code, displayName, owner, members, history);
  }

  public ChatGroup(String code, String displayName, ChatMember owner) {
    this(code, displayName, owner, new HashSet<>(), new ChatHistory<>());
  }

  public UUID getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getDisplayName() {
    return displayName;
  }

  public ChatMember getOwner() {
    return owner;
  }

  public Set<ChatMember> getMembers() {
    return new HashSet<>(members);
  }

  public void addMember(ChatMember member) {
    this.members.add(member);
  }

  public ChatHistory<ChatGroupMessage> getHistory() {
    return history;
  }

  public Optional<ChatGroupMessage> getLatestMessage() {
    return history.getLatestMessage();
  }

  public Set<ChatMember> getOnlineMembers() {
    return getMembers()
            .stream()
            .filter(ChatMember::isReachable)
            .collect(Collectors.toUnmodifiableSet());
  }

  public abstract void sendMessage(ChatMember author, String message);

}
