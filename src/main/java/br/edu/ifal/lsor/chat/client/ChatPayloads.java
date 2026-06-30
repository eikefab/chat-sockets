package br.edu.ifal.lsor.chat.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ChatPayloads {

  private ChatPayloads() {}

  public static List<Map<String, Serializable>> listFromPayload(
      Map<String, Serializable> payload, String key) {
    Serializable value = payload.get(key);
    if (!(value instanceof List<?> list)) {
      return List.of();
    }
    List<Map<String, Serializable>> result = new ArrayList<>();
    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
        result.add(stringSerializableMap(map, key));
      }
    }
    return List.copyOf(result);
  }

  private static Map<String, Serializable> stringSerializableMap(Map<?, ?> map, String parentKey) {
    Map<String, Serializable> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String key)
          || !(entry.getValue() instanceof Serializable value)) {
        throw new IllegalArgumentException("Payload com item inválido na lista: " + parentKey);
      }
      result.put(key, value);
    }
    return Map.copyOf(result);
  }

  public static String string(Map<String, Serializable> payload, String key) {
    Serializable value = payload.get(key);
    if (value instanceof String text) {
      return text;
    }
    throw new IllegalArgumentException("Payload sem campo textual obrigatório: " + key);
  }

  public static Optional<String> optionalString(Map<String, Serializable> payload, String key) {
    Serializable value = payload.get(key);
    if (value == null) {
      return Optional.empty();
    }
    if (value instanceof String text) {
      return Optional.of(text);
    }
    throw new IllegalArgumentException("Payload com campo textual inválido: " + key);
  }

  public static boolean booleanValue(
      Map<String, Serializable> payload, String key, boolean defaultValue) {
    Serializable value = payload.get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean flag) {
      return flag;
    }
    throw new IllegalArgumentException("Payload com campo booleano inválido: " + key);
  }

  public static int intValue(Map<String, Serializable> payload, String key, int defaultValue) {
    Serializable value = payload.get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Integer number) {
      return number;
    }
    throw new IllegalArgumentException("Payload com campo inteiro inválido: " + key);
  }

  public static List<String> stringList(Map<String, Serializable> payload, String key) {
    Serializable value = payload.get(key);
    if (value == null) {
      return List.of();
    }
    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException("Payload com lista textual inválida: " + key);
    }
    List<String> result = new ArrayList<>();
    for (Object item : list) {
      if (!(item instanceof String text)) {
        throw new IllegalArgumentException("Payload com item textual inválido na lista: " + key);
      }
      result.add(text);
    }
    return List.copyOf(result);
  }

  public static java.time.Instant instant(Map<String, Serializable> payload, String key) {
    Serializable value = payload.get(key);
    if (value instanceof java.time.Instant instant) {
      return instant;
    }
    throw new IllegalArgumentException("Payload sem campo temporal obrigatório: " + key);
  }
}
