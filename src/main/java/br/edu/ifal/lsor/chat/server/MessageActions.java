package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class MessageActions {

  private final ChatState state;
  private final EventFactory events;

  MessageActions(ChatState state, EventFactory events) {
    this.state = state;
    this.events = events;
  }

  ChatAction forAction(String action) {
    return switch (action) {
      case Actions.SEND_DIRECT -> this::sendDirect;
      case Actions.SEND_GROUP -> this::sendGroup;
      case Actions.GET_HISTORY -> this::getHistory;
      default -> null;
    };
  }

  ServiceResult sendDirect(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException {
    ActionPayloads.SendDirectPayload direct = payload.sendDirect();
    String targetUsername = direct.targetUsername();
    String text = direct.text();
    if (session.username().equals(targetUsername)) {
      return new ServiceResult(
          ServerResponse.error(
              request.requestId(), Codes.CANNOT_MESSAGE_SELF, "Não envie mensagem para si mesmo."));
    }
    if (state.findUser(targetUsername) == null) {
      return new ServiceResult(
          ServerResponse.error(
              request.requestId(), Codes.USER_NOT_FOUND, "Usuário não encontrado."));
    }
    if (!state.isOnline(targetUsername)) {
      return new ServiceResult(
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
            Payloads.messageAccepted(message.messageId(), message.createdAt(), true)),
        List.of(
            events.toUsers(
                Events.DIRECT_MESSAGE,
                Payloads.directMessageEvent(
                    message.messageId(), session.username(), text, message.createdAt()),
                Set.of(targetUsername))),
        false);
  }

  ServiceResult sendGroup(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    ActionPayloads.SendGroupPayload groupPayload = payload.sendGroup();
    String groupCode = groupPayload.groupCode();
    String text = groupPayload.text();
    GroupRecord group = state.requireGroup(groupCode);
    if (!group.hasMember(session.username())) {
      return new ServiceResult(
          ServerResponse.error(
              request.requestId(), Codes.NOT_GROUP_MEMBER, "Usuário não participa do grupo."));
    }
    MessageRecord message =
        MessageRecord.group(
            UUID.randomUUID(), session.username(), group.groupCode(), text, Instant.now());
    group.addMessage(message);
    Set<String> onlineMembers = state.onlineMembers(group.memberUsernames());
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(),
            Codes.MESSAGE_ACCEPTED,
            "Mensagem aceita.",
            Payloads.messageAcceptedWithOnlineRecipients(
                message.messageId(), message.createdAt(), onlineMembers.size())),
        List.of(
            events.toUsers(
                Events.GROUP_MESSAGE,
                Payloads.groupMessageEvent(
                    message.messageId(),
                    group.groupCode(),
                    group.displayName(),
                    session.username(),
                    text,
                    message.createdAt()),
                onlineMembers)),
        false);
  }

  ServiceResult getHistory(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    ActionPayloads.HistoryPayload history = payload.history(100);
    MessageScope scope = history.scope();
    String target = history.target();
    int limit = history.limit();
    List<MessageRecord> source = historySource(session, scope, target);
    int fromIndex = Math.max(0, source.size() - limit);
    List<Map<String, Serializable>> messages =
        source.subList(fromIndex, source.size()).stream().map(MessageRecord::toPayload).toList();
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(),
            Codes.HISTORY_RETURNED,
            "Histórico retornado.",
            Payloads.historyResult(scope.name(), target, messages)));
  }

  private List<MessageRecord> historySource(ChatSession session, MessageScope scope, String target)
      throws ServiceFailureException {
    return switch (scope) {
      case DIRECT -> directHistory(session, target);
      case GROUP -> groupHistory(session, target);
    };
  }

  private List<MessageRecord> directHistory(ChatSession session, String target)
      throws ServiceFailureException {
    if (state.findUser(target) == null) {
      throw new ServiceFailureException(Codes.USER_NOT_FOUND, "Usuário não encontrado.");
    }
    return state.directHistoryBetween(session.username(), target);
  }

  private List<MessageRecord> groupHistory(ChatSession session, String target)
      throws ServiceFailureException {
    GroupRecord group = state.requireGroup(target);
    if (!group.hasMember(session.username())) {
      throw new ServiceFailureException(Codes.NOT_GROUP_MEMBER, "Usuário não participa do grupo.");
    }
    return group.historySnapshot();
  }
}
