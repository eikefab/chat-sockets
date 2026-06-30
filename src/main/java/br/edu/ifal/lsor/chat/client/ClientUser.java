package br.edu.ifal.lsor.chat.client;

import java.io.Serializable;
import java.util.Map;

public record ClientUser(String username, String displayName, boolean online) {

  public static ClientUser fromPayload(Map<String, Serializable> payload) {
    String username = (String) payload.get("username");
    String displayName = (String) payload.get("displayName");
    Boolean online = (Boolean) payload.get("online");
    return new ClientUser(
        username, displayName != null ? displayName : username, online == null || online);
  }

  public String label() {
    if (displayName == null || displayName.equals(username)) {
      return username;
    }
    return displayName + " (" + username + ")";
  }
}
