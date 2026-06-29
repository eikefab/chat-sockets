package br.edu.ifal.lsor.chat.server;

record ConversationTarget(MessageScope scope, String value) {

  static ConversationTarget direct(String username) {
    return new ConversationTarget(MessageScope.DIRECT, username);
  }

  static ConversationTarget group(String groupCode) {
    return new ConversationTarget(MessageScope.GROUP, groupCode);
  }

  boolean isDirectBetween(String firstUsername, String secondUsername) {
    return scope == MessageScope.DIRECT
        && (value.equals(firstUsername) || value.equals(secondUsername));
  }
}
