package br.edu.ifal.lsor.chat.client;

public interface ChatClientListener {

  default void onDirectMessage(ChatMessage message) {}

  default void onGroupMessage(ChatMessage message) {}

  default void onUserOnline(ClientUser user) {}

  default void onUserOffline(String username) {}

  default void onGroupsChanged() {}

  default void onSystemMessage(String message) {}

  default void onDisconnected() {}
}
