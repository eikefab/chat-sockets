package br.edu.ifal.lsor.chat.client;

public record ConversationTarget(ConversationKind kind, String id, String label) {

  public static ConversationTarget user(ClientUser user) {
    return new ConversationTarget(ConversationKind.DIRECT, user.username(), user.label());
  }

  public static ConversationTarget group(ClientGroup group) {
    return new ConversationTarget(ConversationKind.GROUP, group.groupCode(), group.label());
  }

  @Override
  public String toString() {
    return label;
  }
}
