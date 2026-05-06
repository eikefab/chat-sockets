package br.edu.ifal.lsor.chat;

import java.util.LinkedList;
import java.util.Optional;

public class ChatHistory<T extends ChatMessage> {

  private final LinkedList<T> history;

  public ChatHistory(LinkedList<T> history) {
    this.history = history;
  }

  public ChatHistory() {
    this(new LinkedList<>());
  }

  public LinkedList<T> getHistory() {
    return new LinkedList<>(history);
  }

  public Optional<T> getLatestMessage() {
    return Optional.ofNullable(history.peekLast());
  }

  public void addMessage(T message) {
    this.history.addLast(message);
  }
}
