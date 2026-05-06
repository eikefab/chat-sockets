package br.edu.ifal.lsor.chat.member;

import br.edu.ifal.lsor.chat.ChatHistory;
import br.edu.ifal.lsor.chat.group.ChatGroup;

import java.util.LinkedHashSet;
import java.util.UUID;

public abstract class ChatMember {

  private final UUID id;
  private final String username;
  private final String displayName;
  private final ChatHistory<ChatMemberMessage> history;

  public ChatMember(UUID id, String username, String displayName, ChatHistory<ChatMemberMessage> history) {
    this.id = id;
    this.username = username;
    this.displayName = displayName;
    this.history = history;
  }

  public ChatMember(String username, String displayName) {
    this(UUID.randomUUID(), username, displayName, new ChatHistory<>());
  }

  public UUID getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public ChatHistory<ChatMemberMessage> getHistory() {
    return history;
  }

  public abstract boolean isReachable();
  public abstract void heartbeat();
  public abstract void sendMessage(String text);

  public abstract void chat(String text, ChatGroup group);
  public abstract void chat(String text, ChatMember target);

  public abstract LinkedHashSet<ChatGroup> getGroups();
}
