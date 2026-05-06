package br.edu.ifal.lsor.chat.member;

import br.edu.ifal.lsor.chat.ChatMessage;

import java.time.Instant;
import java.util.UUID;

public class ChatMemberMessage extends ChatMessage {

  private final ChatMember to;
  private final ChatMember from;

  public ChatMemberMessage(ChatMember to, ChatMember from, UUID messageId, String message, Instant createdAt) {
    super(messageId, message, createdAt);

    this.to = to;
    this.from = from;
  }

  public ChatMemberMessage(ChatMember to, ChatMember from, String message) {
    this(to, from, UUID.randomUUID(), message, Instant.now());
  }

  public ChatMember getTo() {
    return to;
  }

  public ChatMember getFrom() {
    return from;
  }

}
