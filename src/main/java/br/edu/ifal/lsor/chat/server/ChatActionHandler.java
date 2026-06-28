package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.Protocol;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ChatActionHandler {

  private final ChatState state = new ChatState();
  private final EventFactory events = new EventFactory(state);
  private final GroupActions groupActions;
  private final SessionActions sessionActions;
  private final MessageActions messageActions;
  private final Map<String, ChatAction> dispatch = new HashMap<>();

  ChatActionHandler() {
    this.sessionActions = new SessionActions(state, events);
    this.groupActions = new GroupActions(state, events);
    this.messageActions = new MessageActions(state, events, groupActions);
    register(Actions.LOGIN, sessionActions.forAction(Actions.LOGIN));
    register(Actions.LOGOUT, sessionActions.forAction(Actions.LOGOUT));
    register(Actions.HEARTBEAT, sessionActions.forAction(Actions.HEARTBEAT));
    register(Actions.LIST_USERS, sessionActions.forAction(Actions.LIST_USERS));
    register(Actions.LIST_GROUPS, groupActions.forAction(Actions.LIST_GROUPS));
    register(Actions.CREATE_GROUP, groupActions.forAction(Actions.CREATE_GROUP));
    register(Actions.JOIN_GROUP, groupActions.forAction(Actions.JOIN_GROUP));
    register(Actions.LEAVE_GROUP, groupActions.forAction(Actions.LEAVE_GROUP));
    register(Actions.RENAME_GROUP, groupActions.forAction(Actions.RENAME_GROUP));
    register(Actions.DELETE_GROUP, groupActions.forAction(Actions.DELETE_GROUP));
    register(Actions.SEND_DIRECT, messageActions.forAction(Actions.SEND_DIRECT));
    register(Actions.SEND_GROUP, messageActions.forAction(Actions.SEND_GROUP));
    register(Actions.GET_HISTORY, messageActions.forAction(Actions.GET_HISTORY));
  }

  private void register(String action, ChatAction handler) {
    dispatch.put(action, handler);
  }

  public synchronized ServiceResult handle(ChatSession session, ClientRequest request) {
    if (request == null || request.requestId() == null || request.action() == null) {
      return new ServiceResult(
          ServerResponse.error(null, Codes.INVALID_PAYLOAD, "Requisição inválida."));
    }
    if (!Protocol.VERSION.equals(request.protocolVersion())) {
      return new ServiceResult(
          ServerResponse.error(
              request.requestId(), Codes.INVALID_PAYLOAD, "Versão de protocolo inválida."));
    }
    if (!Actions.LOGIN.equals(request.action()) && !session.isAuthenticated()) {
      return new ServiceResult(
          ServerResponse.error(
              request.requestId(), Codes.AUTH_REQUIRED, "Faça login antes de continuar."));
    }

    ChatAction action = dispatch.get(request.action());
    if (action == null) {
      return new ServiceResult(
          ServerResponse.error(request.requestId(), Codes.UNKNOWN_ACTION, "Ação desconhecida."));
    }

    try {
      PayloadReader payload = PayloadReader.from(request);
      return action.handle(session, request, payload);
    } catch (InvalidPayloadException exception) {
      return new ServiceResult(
          ServerResponse.error(request.requestId(), Codes.INVALID_PAYLOAD, exception.getMessage()));
    } catch (ServiceFailureException exception) {
      return new ServiceResult(
          ServerResponse.error(request.requestId(), exception.code(), exception.getMessage()));
    }
  }

  public synchronized List<OutboundEvent> disconnect(ChatSession session) {
    return sessionActions.disconnect(session);
  }
}
