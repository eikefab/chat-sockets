package br.edu.ifal.lsor.chat.client;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public final class ChatPayloads {

  private ChatPayloads() {}

  @SuppressWarnings("unchecked")
  public static List<Map<String, Serializable>> listFromPayload(
      Map<String, Serializable> payload, String key) {
    Serializable value = payload.get(key);
    if (value instanceof List<?> list) {
      return (List<Map<String, Serializable>>) list;
    }
    return List.of();
  }
}
