package br.edu.ifal.lsor.chat.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

final class MessageHistory {

  private final Deque<MessageRecord> messages = new ArrayDeque<>();

  void add(MessageRecord message) {
    messages.addLast(message);
    while (messages.size() > PayloadLimits.MAX_HISTORY_MESSAGES) {
      messages.removeFirst();
    }
  }

  List<MessageRecord> snapshot() {
    return List.copyOf(messages);
  }

  List<MessageRecord> between(String firstUsername, String secondUsername) {
    return messages.stream()
        .filter(message -> message.isDirectBetween(firstUsername, secondUsername))
        .toList();
  }
}
