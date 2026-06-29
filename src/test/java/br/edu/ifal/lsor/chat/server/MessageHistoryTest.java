package br.edu.ifal.lsor.chat.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageHistoryTest {

  @Test
  void retainsOnlyLatestMessages() {
    MessageHistory history = new MessageHistory();

    for (int index = 0; index < PayloadLimits.MAX_HISTORY_MESSAGES + 1; index++) {
      history.add(message("alice", "bob", "msg " + index));
    }

    List<MessageRecord> snapshot = history.snapshot();
    assertEquals(PayloadLimits.MAX_HISTORY_MESSAGES, snapshot.size());
    assertEquals("msg 1", snapshot.get(0).text());
  }

  @Test
  void filtersDirectConversationWithoutLeakingOtherTargets() {
    MessageHistory history = new MessageHistory();
    history.add(message("alice", "bob", "one"));
    history.add(message("alice", "carol", "two"));
    history.add(message("bob", "alice", "three"));

    List<MessageRecord> messages = history.between("alice", "bob");

    assertEquals(2, messages.size());
    assertEquals("one", messages.get(0).text());
    assertEquals("three", messages.get(1).text());
  }

  private static MessageRecord message(String fromUsername, String toUsername, String text) {
    return MessageRecord.direct(UUID.randomUUID(), fromUsername, toUsername, text, Instant.now());
  }
}
