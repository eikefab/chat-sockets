package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

final class GroupActions {

  private final ChatState state;
  private final EventFactory events;

  GroupActions(ChatState state, EventFactory events) {
    this.state = state;
    this.events = events;
  }

  ChatAction forAction(String action) {
    return switch (action) {
      case Actions.LIST_GROUPS -> this::listGroups;
      case Actions.CREATE_GROUP -> this::createGroup;
      case Actions.JOIN_GROUP -> this::joinGroup;
      case Actions.LEAVE_GROUP -> this::leaveGroup;
      case Actions.RENAME_GROUP -> this::renameGroup;
      case Actions.DELETE_GROUP -> this::deleteGroup;
      default -> null;
    };
  }

  ServiceResult listGroups(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException {
    ActionPayloads.ListGroupsPayload listPayload = payload.listGroups();
    boolean onlyMine = listPayload.onlyMine();
    List<Map<String, Serializable>> groups =
        state.groupsSortedByCode().stream()
            .filter(group -> !onlyMine || group.hasMember(session.username()))
            .map(group -> Payloads.groupSummary(group, session.username()))
            .toList();
    return new ServiceResult(
        ServerResponse.ok(
            request.requestId(),
            Codes.GROUPS_LISTED,
            "Grupos listados.",
            Map.of("groups", Payloads.list(groups))));
  }

  ServiceResult createGroup(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException {
    ActionPayloads.GroupDisplayPayload groupDisplay = payload.groupDisplay();
    String groupCode = groupDisplay.groupCode();
    String displayName = groupDisplay.displayName();
    if (state.findGroup(groupCode) != null) {
      return new ServiceResult(
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

  ServiceResult joinGroup(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    GroupRecord group = requiredGroup(request, payload);
    if (!group.addMember(session.username())) {
      return new ServiceResult(
          ServerResponse.error(
              request.requestId(), Codes.ALREADY_GROUP_MEMBER, "Usuário já participa do grupo."));
    }
    UserRecord user = state.findUser(session.username());
    Map<String, Serializable> responsePayload =
        Map.of(
            "groupId", group.groupId(),
            "groupCode", group.groupCode(),
            "displayName", group.displayName());
    return memberEventResult(
        request,
        Codes.GROUP_JOINED,
        "Usuário entrou no grupo.",
        responsePayload,
        Events.GROUP_MEMBER_JOINED,
        group,
        user);
  }

  ServiceResult leaveGroup(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    GroupRecord group = requiredGroup(request, payload);
    if (!group.hasMember(session.username())) {
      return new ServiceResult(
          ServerResponse.error(
              request.requestId(), Codes.NOT_GROUP_MEMBER, "Usuário não participa do grupo."));
    }
    if (group.ownerUsername().equals(session.username())) {
      return new ServiceResult(
          ServerResponse.error(
              request.requestId(), Codes.OWNER_CANNOT_LEAVE, "Dono deve excluir o grupo."));
    }
    group.removeMember(session.username());
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

  ServiceResult renameGroup(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    GroupRecord group = requiredGroup(request, payload);
    String displayName = payload.groupDisplay().displayName();
    if (!group.ownerUsername().equals(session.username())) {
      return new ServiceResult(
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

  ServiceResult deleteGroup(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    GroupRecord group = requiredGroup(request, payload);
    if (!group.ownerUsername().equals(session.username())) {
      return new ServiceResult(
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
                Map.of(
                    "groupCode", group.groupCode(),
                    "deletedByUsername", session.username()))),
        false);
  }

  GroupRecord requiredGroup(ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException {
    return findGroupOrError(request, payload.groupCode().groupCode());
  }

  GroupRecord findGroupOrError(ClientRequest request, String groupCode)
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
                Payloads.groupJoinLeaveEvent(
                    group.groupCode(), user.username(), user.displayName()),
                group.memberUsernames())),
        false);
  }
}
