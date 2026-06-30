package br.edu.ifal.lsor.chat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChatPayloadsTest {

  @Test
  void listFromPayloadExtractsListSafely() {
    Map<String, Serializable> payload =
        Map.of("items", (Serializable) List.of(Map.of("key", "value")));

    List<Map<String, Serializable>> result = ChatPayloads.listFromPayload(payload, "items");

    assertEquals(1, result.size());
    assertEquals("value", result.get(0).get("key"));
  }

  @Test
  void listFromPayloadReturnsEmptyWhenKeyMissing() {
    Map<String, Serializable> payload = Map.of();

    List<Map<String, Serializable>> result = ChatPayloads.listFromPayload(payload, "other");

    assertTrue(result.isEmpty());
  }

  @Test
  void listFromPayloadReturnsEmptyWhenValueIsNotList() {
    Map<String, Serializable> payload = Map.of("items", "not-a-list");

    List<Map<String, Serializable>> result = ChatPayloads.listFromPayload(payload, "items");

    assertTrue(result.isEmpty());
  }

  @Test
  void stringListExtractsStrings() {
    Map<String, Serializable> payload = Map.of("members", (Serializable) List.of("maria", "joao"));

    List<String> result = ChatPayloads.stringList(payload, "members");

    assertEquals(List.of("maria", "joao"), result);
  }

  @Test
  void stringListRejectsNonListValue() {
    Map<String, Serializable> payload = Map.of("members", "maria");

    assertThrows(IllegalArgumentException.class, () -> ChatPayloads.stringList(payload, "members"));
  }

  @Test
  void stringListRejectsNonStringItems() {
    Map<String, Serializable> payload = Map.of("members", (Serializable) List.of("maria", 1));

    assertThrows(IllegalArgumentException.class, () -> ChatPayloads.stringList(payload, "members"));
  }
}
