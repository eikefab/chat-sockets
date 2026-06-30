package br.edu.ifal.lsor.chat.client;

import java.io.Serializable;
import java.util.Map;

public record ClientGroup(
    String groupCode, String displayName, String ownerUsername, int memberCount, boolean member) {

  public static ClientGroup fromPayload(Map<String, Serializable> payload) {
    String groupCode = (String) payload.get("groupCode");
    String displayName = (String) payload.get("displayName");
    String ownerUsername = (String) payload.get("ownerUsername");
    Integer memberCount = (Integer) payload.get("memberCount");
    Boolean member = (Boolean) payload.get("isMember");
    return new ClientGroup(
        groupCode,
        displayName != null ? displayName : groupCode,
        ownerUsername,
        memberCount != null ? memberCount : 0,
        Boolean.TRUE.equals(member));
  }

  public String label() {
    return "#"
        + groupCode
        + (displayName != null && !displayName.equals(groupCode) ? " - " + displayName : "");
  }
}
