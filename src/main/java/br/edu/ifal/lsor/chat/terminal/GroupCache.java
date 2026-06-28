package br.edu.ifal.lsor.chat.terminal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class GroupCache {

  private final Map<String, String> cache = new ConcurrentHashMap<>();

  boolean contains(String groupCode) {
    return cache.containsKey(groupCode);
  }

  void populate(List<Map<String, Serializable>> groups) {
    cache.clear();
    for (Map<String, Serializable> group : groups) {
      String code = (String) group.get("groupCode");
      String display = (String) group.get("displayName");
      if (code != null) {
        cache.put(code, display != null ? display : code);
      }
    }
  }

  void put(String groupCode, String displayName) {
    cache.put(groupCode, displayName != null ? displayName : groupCode);
  }

  void remove(String groupCode) {
    cache.remove(groupCode);
  }

  String displayName(String groupCode) {
    return cache.getOrDefault(groupCode, groupCode);
  }
}
