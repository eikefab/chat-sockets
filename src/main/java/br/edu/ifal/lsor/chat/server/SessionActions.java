package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

final class SessionActions {

  private final ChatState state;
  private final EventFactory events;

  SessionActions(ChatState state, EventFactory events) {
    this.state = state;
    this.events = events;
  }

  ChatAction forAction(String action) {
    return switch (action) {
      case Actions.LOGIN -> this::handleLogin;
      case Actions.LOGOUT -> this::handleLogout;
      case Actions.HEARTBEAT -> this::handleHeartbeat;
      case Actions.LIST_USERS -> this::handleListUsers;
      default -> null;
    };
  }

  ServiceResult handleLogin(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException {
    if (session.isAuthenticated()) {
      return new ServiceResult(
          ServerResponse.error(
              request.requestId(), Codes.INVALID_PAYLOAD, "Sessão já autenticada."));
    }
    ActionPayloads.LoginPayload loginPayload = payload.login();
    String username = loginPayload.username();
    String displayName = loginPayload.displayName();
    if (state.isOnline(username)) {
      return new ServiceResult(
          ServerResponse.error(
              request.requestId(),
              Codes.USERNAME_ALREADY_ONLINE,
              "Nome de usuário já está online."));
    }
    UserRecord user = state.saveUser(username, displayName);
    session.authenticate(user.memberId(), user.username(), user.displayName());
    state.connect(user, session);
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(), Codes.LOGIN_ACCEPTED, "Login realizado.", Payloads.login(user)),
        List.of(events.userOnline(user)),
        false);
  }

  ServiceResult handleLogout(ChatSession session, ClientRequest request, PayloadReader payload) {
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(), Codes.LOGOUT_ACCEPTED, "Logout realizado.", Map.of()),
        disconnect(session),
        true);
  }

  ServiceResult handleHeartbeat(ChatSession session, ClientRequest request, PayloadReader payload) {
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(),
            Codes.HEARTBEAT_ACK,
            "Heartbeat confirmado.",
            Map.of("serverTime", Instant.now())));
  }

  ServiceResult handleListUsers(ChatSession session, ClientRequest request, PayloadReader payload) {
    List<Map<String, Serializable>> users =
        state.usersSortedByUsername().stream()
            .map(user -> Payloads.user(user, state.isOnline(user.username())))
            .toList();
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(),
            Codes.USERS_LISTED,
            "Usuários listados.",
            Map.of("users", Payloads.list(users))));
  }

  List<OutboundEvent> disconnect(ChatSession session) {
    UserRecord user = state.disconnect(session);
    return user == null ? List.of() : List.of(events.userOffline(user));
  }
}
