package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.Protocol;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class ChatActionHandler {

  private final ChatState state = new ChatState();
  private final EventFactory events = new EventFactory(state);

  public synchronized ServiceResult handle(ChatSession session, ClientRequest request) {
    if (request == null || request.requestId() == null || request.action() == null) {
      return result(ServerResponse.error(null, Codes.INVALID_PAYLOAD, "Requisição inválida."));
    }
    if (!Protocol.VERSION.equals(request.protocolVersion())) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.INVALID_PAYLOAD, "Versão de protocolo inválida."));
    }
    if (!Actions.LOGIN.equals(request.action()) && !session.isAuthenticated()) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.AUTH_REQUIRED, "Faça login antes de continuar."));
    }

    try {
      PayloadReader payload = PayloadReader.from(request);
      return switch (request.action()) {
        case Actions.LOGIN -> login(session, request, payload);
        case Actions.LOGOUT -> logout(session, request);
        case Actions.HEARTBEAT -> heartbeat(request);
        case Actions.LIST_USERS -> listUsers(request);
        case Actions.LIST_GROUPS -> listGroups(session, request, payload);
        case Actions.CREATE_GROUP -> createGroup(session, request, payload);
        case Actions.JOIN_GROUP -> joinGroup(session, request, payload);
        case Actions.LEAVE_GROUP -> leaveGroup(session, request, payload);
        case Actions.RENAME_GROUP -> renameGroup(session, request, payload);
        case Actions.DELETE_GROUP -> deleteGroup(session, request, payload);
        case Actions.SEND_DIRECT -> sendDirect(session, request, payload);
        case Actions.SEND_GROUP -> sendGroup(session, request, payload);
        case Actions.GET_HISTORY -> getHistory(session, request, payload);
        default ->
            result(
                ServerResponse.error(
                    request.requestId(), Codes.UNKNOWN_ACTION, "Ação desconhecida."));
      };
    } catch (InvalidPayloadException exception) {
      return result(
          ServerResponse.error(request.requestId(), Codes.INVALID_PAYLOAD, exception.getMessage()));
    } catch (ServiceFailureException exception) {
      return result(
          ServerResponse.error(request.requestId(), exception.code(), exception.getMessage()));
    }
  }

  public synchronized List<OutboundEvent> disconnect(ChatSession session) {
    UserRecord user = state.disconnect(session);
    return user == null ? List.of() : List.of(events.userOffline(user));
  }

  private ServiceResult login(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException {
    if (session.isAuthenticated()) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.INVALID_PAYLOAD, "Sessão já autenticada."));
    }
    String username = payload.requiredString("username");
    String displayName = payload.requiredString("displayName");
    if (state.isOnline(username)) {
      return result(
          ServerResponse.error(
              request.requestId(),
              Codes.USERNAME_ALREADY_ONLINE,
              "Nome de usuário já está online."));
    }
    UserRecord user = state.saveUser(username, displayName);
    session.authenticate(user.memberId(), user.username(), user.displayName());
    state.connect(user, session);
    Map<String, Serializable> responsePayload =
        Map.of(
            "memberId",
            user.memberId(),
            "username",
            user.username(),
            "displayName",
            user.displayName());
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(), Codes.LOGIN_ACCEPTED, "Login realizado.", responsePayload),
        List.of(events.userOnline(user)),
        false);
  }

  private ServiceResult logout(ChatSession session, ClientRequest request) {
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(), Codes.LOGOUT_ACCEPTED, "Logout realizado.", Map.of()),
        disconnect(session),
        true);
  }

  private ServiceResult heartbeat(ClientRequest request) {
    return result(
        ServerResponse.ok(
            request.requestId(),
            Codes.HEARTBEAT_ACK,
            "Heartbeat confirmado.",
            Map.of("serverTime", Instant.now())));
  }

  private ServiceResult listUsers(ClientRequest request) {
    List<Map<String, Serializable>> users =
        state.usersSortedByUsername().stream()
            .map(user -> Payloads.user(user, state.isOnline(user.username())))
            .toList();
    return result(
        ServerResponse.ok(
            request.requestId(),
            Codes.USERS_LISTED,
            "Usuários listados.",
            Map.of("users", Payloads.list(users))));
  }

  private ServiceResult listGroups(
      ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException {
    boolean onlyMine = payload.optionalBoolean("onlyMine", false);
    List<Map<String, Serializable>> groups =
        state.groupsSortedByCode().stream()
            .filter(group -> !onlyMine || group.members().contains(session.username()))
            .map(group -> Payloads.groupSummary(group, session.username()))
            .toList();
    return result(
        ServerResponse.ok(
            request.requestId(),
            Codes.GROUPS_LISTED,
            "Grupos listados.",
            Map.of("groups", Payloads.list(groups))));
  }

  private ServiceResult createGroup(
      ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException {
    String groupCode = payload.requiredString("groupCode");
    String displayName = payload.requiredString("displayName");
    if (state.findGroup(groupCode) != null) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.GROUP_ALREADY_EXISTS, "Grupo já existe."));
    }
    GroupRecord group = state.createGroup(groupCode, displayName, session.username());
    Map<String, Serializable> groupPayload = Payloads.group(group);
    return new ServiceResult(
        ServerResponse.ok(request.requestId(), Codes.GROUP_CREATED, "Grupo criado.", groupPayload),
        List.of(events.broadcast(Events.GROUP_CREATED, groupPayload)),
        false);
  }

  private ServiceResult joinGroup(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    GroupRecord group = requiredGroup(request, payload);
    if (!group.members().add(session.username())) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.ALREADY_GROUP_MEMBER, "Usuário já participa do grupo."));
    }
    UserRecord user = state.findUser(session.username());
    Map<String, Serializable> responsePayload =
        Map.of(
            "groupId",
            group.groupId(),
            "groupCode",
            group.groupCode(),
            "displayName",
            group.displayName());
    return memberEventResult(
        request,
        Codes.GROUP_JOINED,
        "Usuário entrou no grupo.",
        responsePayload,
        Events.GROUP_MEMBER_JOINED,
        group,
        user);
  }

  private ServiceResult leaveGroup(
      ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    GroupRecord group = requiredGroup(request, payload);
    if (!group.members().contains(session.username())) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.NOT_GROUP_MEMBER, "Usuário não participa do grupo."));
    }
    if (group.ownerUsername().equals(session.username())) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.OWNER_CANNOT_LEAVE, "Dono deve excluir o grupo."));
    }
    group.members().remove(session.username());
    UserRecord user = state.findUser(session.username());
    return memberEventResult(
        request,
        Codes.GROUP_LEFT,
        "Usuário saiu do grupo.",
        Map.of("groupCode", group.groupCode()),
        Events.GROUP_MEMBER_LEFT,
        group,
        user);
  }

  private ServiceResult renameGroup(
      ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    GroupRecord group = requiredGroup(request, payload);
    String displayName = payload.requiredString("displayName");
    if (!group.ownerUsername().equals(session.username())) {
      return result(
          ServerResponse.error(
              request.requestId(),
              Codes.PERMISSION_DENIED,
              "Apenas o dono pode renomear o grupo."));
    }
    group.rename(displayName);
    Map<String, Serializable> groupPayload = Payloads.group(group);
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(), Codes.GROUP_RENAMED, "Grupo renomeado.", groupPayload),
        List.of(events.broadcast(Events.GROUP_RENAMED, groupPayload)),
        false);
  }

  private ServiceResult deleteGroup(
      ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    GroupRecord group = requiredGroup(request, payload);
    if (!group.ownerUsername().equals(session.username())) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.PERMISSION_DENIED, "Apenas o dono pode excluir o grupo."));
    }
    state.removeGroup(group);
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(),
            Codes.GROUP_DELETED,
            "Grupo excluído.",
            Map.of("groupCode", group.groupCode())),
        List.of(
            events.broadcast(
                Events.GROUP_DELETED,
                Map.of("groupCode", group.groupCode(), "deletedByUsername", session.username()))),
        false);
  }

  private ServiceResult sendDirect(
      ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException {
    String targetUsername = payload.requiredString("targetUsername");
    String text = payload.requiredString("text");
    if (session.username().equals(targetUsername)) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.CANNOT_MESSAGE_SELF, "Não envie mensagem para si mesmo."));
    }
    if (state.findUser(targetUsername) == null) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.USER_NOT_FOUND, "Usuário não encontrado."));
    }
    if (!state.isOnline(targetUsername)) {
      return result(
          ServerResponse.error(request.requestId(), Codes.USER_OFFLINE, "Usuário está offline."));
    }
    MessageRecord message =
        MessageRecord.direct(
            UUID.randomUUID(), session.username(), targetUsername, text, Instant.now());
    state.addDirectMessage(message);
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(),
            Codes.MESSAGE_ACCEPTED,
            "Mensagem aceita.",
            Map.of(
                "messageId",
                message.messageId(),
                "createdAt",
                message.createdAt(),
                "deliveredToOnline",
                true)),
        List.of(
            events.toUsers(
                Events.DIRECT_MESSAGE,
                Map.of(
                    "messageId", message.messageId(),
                    "fromUsername", session.username(),
                    "text", text,
                    "createdAt", message.createdAt()),
                Set.of(targetUsername))),
        false);
  }

  private ServiceResult sendGroup(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    String groupCode = payload.requiredString("groupCode");
    String text = payload.requiredString("text");
    GroupRecord group = findGroupOrError(request, groupCode);
    if (!group.members().contains(session.username())) {
      return result(
          ServerResponse.error(
              request.requestId(), Codes.NOT_GROUP_MEMBER, "Usuário não participa do grupo."));
    }
    MessageRecord message =
        MessageRecord.group(
            UUID.randomUUID(), session.username(), group.groupCode(), text, Instant.now());
    group.history().add(message);
    Set<String> onlineMembers = state.onlineMembers(group.members());
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(),
            Codes.MESSAGE_ACCEPTED,
            "Mensagem aceita.",
            Map.of(
                "messageId",
                message.messageId(),
                "createdAt",
                message.createdAt(),
                "onlineRecipients",
                onlineMembers.size())),
        List.of(
            events.toUsers(
                Events.GROUP_MESSAGE,
                Map.of(
                    "messageId", message.messageId(),
                    "groupCode", group.groupCode(),
                    "groupDisplayName", group.displayName(),
                    "authorUsername", session.username(),
                    "text", text,
                    "createdAt", message.createdAt()),
                onlineMembers)),
        false);
  }

  private ServiceResult getHistory(
      ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    String scope = payload.requiredString("scope");
    String target = payload.requiredString("target");
    int limit = payload.optionalLimit("limit", 50, 100);
    List<MessageRecord> source = historySource(session, request, scope, target);
    int fromIndex = Math.max(0, source.size() - limit);
    List<Map<String, Serializable>> messages =
        source.subList(fromIndex, source.size()).stream().map(MessageRecord::toPayload).toList();
    return result(
        ServerResponse.ok(
            request.requestId(),
            Codes.HISTORY_RETURNED,
            "Histórico retornado.",
            Map.of("scope", scope, "target", target, "messages", Payloads.list(messages))));
  }

  private List<MessageRecord> historySource(
      ChatSession session, ClientRequest request, String scope, String target)
      throws InvalidPayloadException, ServiceFailureException {
    if ("DIRECT".equals(scope)) {
      if (state.findUser(target) == null) {
        throw new ServiceFailureException(Codes.USER_NOT_FOUND, "Usuário não encontrado.");
      }
      return state.directHistoryBetween(session.username(), target);
    }
    if ("GROUP".equals(scope)) {
      GroupRecord group = findGroupOrError(request, target);
      if (!group.members().contains(session.username())) {
        throw new ServiceFailureException(
            Codes.NOT_GROUP_MEMBER, "Usuário não participa do grupo.");
      }
      return List.copyOf(group.history());
    }
    throw new InvalidPayloadException("Scope inválido.");
  }

  private GroupRecord requiredGroup(ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    return findGroupOrError(request, payload.requiredString("groupCode"));
  }

  private GroupRecord findGroupOrError(ClientRequest request, String groupCode)
      throws ServiceFailureException {
    GroupRecord group = state.findGroup(groupCode);
    if (group == null) {
      throw new ServiceFailureException(Codes.GROUP_NOT_FOUND, "Grupo não encontrado.");
    }
    return group;
  }

  private ServiceResult memberEventResult(
      ClientRequest request,
      String code,
      String message,
      Map<String, Serializable> responsePayload,
      String eventType,
      GroupRecord group,
      UserRecord user) {
    return new ServiceResult(
        ServerResponse.ok(request.requestId(), code, message, responsePayload),
        List.of(
            events.toUsers(
                eventType,
                Map.of(
                    "groupCode",
                    group.groupCode(),
                    "username",
                    user.username(),
                    "displayName",
                    user.displayName()),
                group.members())),
        false);
  }

  private ServiceResult result(ServerResponse response) {
    return new ServiceResult(response);
  }
}
