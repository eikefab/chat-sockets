package br.edu.ifal.lsor.chat.client;

import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

final class ClientEventMapper {

  Optional<ClientEvent> map(ServerEvent event) {
    Map<String, Serializable> payload = event.payload();
    return switch (event.eventType()) {
      case Events.DIRECT_MESSAGE -> Optional.of(directMessage(payload));
      case Events.GROUP_MESSAGE -> Optional.of(groupMessage(payload));
      case Events.USER_ONLINE ->
          Optional.of(new ClientEvent.UserOnline(ClientUser.fromPayload(payload)));
      case Events.USER_OFFLINE ->
          Optional.of(new ClientEvent.UserOffline(ChatPayloads.string(payload, "username")));
      case Events.GROUP_CREATED -> Optional.of(groupChanged(payload, GroupEventKind.CREATED));
      case Events.GROUP_RENAMED -> Optional.of(groupChanged(payload, GroupEventKind.RENAMED));
      case Events.GROUP_DELETED -> Optional.of(groupChanged(payload, GroupEventKind.DELETED));
      case Events.GROUP_MEMBER_JOINED ->
          Optional.of(groupChanged(payload, GroupEventKind.MEMBER_JOINED));
      case Events.GROUP_MEMBER_LEFT ->
          Optional.of(groupChanged(payload, GroupEventKind.MEMBER_LEFT));
      default -> Optional.empty();
    };
  }

  private ClientEvent directMessage(Map<String, Serializable> payload) {
    String from = ChatPayloads.string(payload, "fromUsername");
    String text = ChatPayloads.string(payload, "text");
    Instant createdAt = ChatPayloads.instant(payload, "createdAt");
    return new ClientEvent.DirectMessage(
        new ChatMessage(ConversationKind.DIRECT, from, from, text, createdAt, false));
  }

  private ClientEvent groupMessage(Map<String, Serializable> payload) {
    String groupCode = ChatPayloads.string(payload, "groupCode");
    String author = ChatPayloads.string(payload, "authorUsername");
    String text = ChatPayloads.string(payload, "text");
    Instant createdAt = ChatPayloads.instant(payload, "createdAt");
    return new ClientEvent.GroupMessage(
        new ChatMessage(ConversationKind.GROUP, groupCode, author, text, createdAt, false));
  }

  private ClientEvent groupChanged(Map<String, Serializable> payload, GroupEventKind kind) {
    return new ClientEvent.GroupsChanged(
        kind, ChatPayloads.optionalString(payload, "groupCode").orElse(""));
  }
}
