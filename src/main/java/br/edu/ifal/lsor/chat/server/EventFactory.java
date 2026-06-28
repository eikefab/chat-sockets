package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class EventFactory {

  private final ChatState state;

  EventFactory(ChatState state) {
    this.state = state;
  }

  OutboundEvent userOnline(UserRecord user) {
    return broadcastExcept(
        Events.USER_ONLINE,
        Map.of(
            "memberId",
            user.memberId(),
            "username",
            user.username(),
            "displayName",
            user.displayName()),
        user.username());
  }

  OutboundEvent userOffline(UserRecord user) {
    return broadcastExcept(
        Events.USER_OFFLINE,
        Map.of("memberId", user.memberId(), "username", user.username()),
        user.username());
  }

  OutboundEvent broadcast(String eventType, Map<String, Serializable> payload) {
    return toUsers(eventType, payload, state.onlineUsernames());
  }

  OutboundEvent broadcastExcept(
      String eventType, Map<String, Serializable> payload, String exceptUsername) {
    Set<String> targets = new HashSet<>(state.onlineUsernames());
    targets.remove(exceptUsername);
    return toUsers(eventType, payload, targets);
  }

  OutboundEvent toUsers(
      String eventType, Map<String, Serializable> payload, Set<String> usernames) {
    return new OutboundEvent(ServerEvent.of(eventType, payload), state.onlineMembers(usernames));
  }
}
