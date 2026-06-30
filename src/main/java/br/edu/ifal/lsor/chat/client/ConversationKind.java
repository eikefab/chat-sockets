package br.edu.ifal.lsor.chat.client;

public enum ConversationKind {
  DIRECT("DIRECT"),
  GROUP("GROUP");

  private final String protocolValue;

  ConversationKind(String protocolValue) {
    this.protocolValue = protocolValue;
  }

  public String protocolValue() {
    return protocolValue;
  }
}
