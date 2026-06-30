package br.edu.ifal.lsor.chat.client;

import java.io.Serializable;
import java.util.Map;

public record ClientGroup(
    String groupCode, String displayName, String ownerUsername, int memberCount, boolean member) {

  public static ClientGroup fromPayload(Map<String, Serializable> payload) {
    String groupCode = ChatPayloads.string(payload, "groupCode");
    String displayName = ChatPayloads.optionalString(payload, "displayName").orElse(groupCode);
    String ownerUsername = ChatPayloads.optionalString(payload, "ownerUsername").orElse("");
    int memberCount = ChatPayloads.intValue(payload, "memberCount", 0);
    boolean member = ChatPayloads.booleanValue(payload, "isMember", false);
    return new ClientGroup(groupCode, displayName, ownerUsername, memberCount, member);
  }

  public String label() {
    return "#"
        + groupCode
        + (displayName != null && !displayName.equals(groupCode) ? " - " + displayName : "");
  }
}
