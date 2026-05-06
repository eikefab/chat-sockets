package br.edu.ifal.lsor.chat.group;

import br.edu.ifal.lsor.chat.ChatMessage;
import br.edu.ifal.lsor.chat.member.ChatMember;

import java.time.Instant;
import java.util.UUID;

public abstract class ChatGroupMessage extends ChatMessage {

  private final ChatMember author;

  public ChatGroupMessage(UUID id, ChatMember author, String message, Instant createdAt) {
    super(id, message, createdAt);

    this.author = author;
  }

  public ChatGroupMessage(ChatMember author, String message, Instant createdAt) {
    super(message, createdAt);

    this.author = author;
  }

  public ChatGroupMessage(ChatMember author, String message) {
    super(message);

    this.author = author;
  }

  public ChatMember getAuthor() {
    return author;
  }

}
