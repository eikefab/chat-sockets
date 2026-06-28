package br.edu.ifal.lsor.chat.server;

import java.util.UUID;

public final class ChatSession {

  private UUID memberId;
  private String username;
  private String displayName;

  public boolean isAuthenticated() {
    return username != null;
  }

  public UUID memberId() {
    return memberId;
  }

  public String username() {
    return username;
  }

  public String displayName() {
    return displayName;
  }

  void authenticate(UUID memberId, String username, String displayName) {
    this.memberId = memberId;
    this.username = username;
    this.displayName = displayName;
  }

  void clear() {
    this.memberId = null;
    this.username = null;
    this.displayName = null;
  }
}
