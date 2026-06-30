package br.edu.ifal.lsor.chat.client;

import java.io.Serializable;
import java.util.Map;

public record ClientUser(String username, String displayName, boolean online) {

  public static ClientUser fromPayload(Map<String, Serializable> payload) {
    String username = ChatPayloads.string(payload, "username");
    String displayName = ChatPayloads.optionalString(payload, "displayName").orElse(username);
    boolean online = ChatPayloads.booleanValue(payload, "online", true);
    return new ClientUser(username, displayName, online);
  }

  public String label() {
    if (displayName == null || displayName.equals(username)) {
      return username;
    }
    return displayName + " (" + username + ")";
  }
}
