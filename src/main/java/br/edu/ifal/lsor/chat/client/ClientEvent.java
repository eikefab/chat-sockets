package br.edu.ifal.lsor.chat.client;

public sealed interface ClientEvent
    permits ClientEvent.DirectMessage,
        ClientEvent.GroupMessage,
        ClientEvent.UserOnline,
        ClientEvent.UserOffline,
        ClientEvent.GroupsChanged,
        ClientEvent.Disconnected {

  record DirectMessage(ChatMessage message) implements ClientEvent {}

  record GroupMessage(ChatMessage message) implements ClientEvent {}

  record UserOnline(ClientUser user) implements ClientEvent {}

  record UserOffline(String username) implements ClientEvent {}

  record GroupsChanged(GroupEventKind kind, String groupCode) implements ClientEvent {}

  record Disconnected() implements ClientEvent {}
}
