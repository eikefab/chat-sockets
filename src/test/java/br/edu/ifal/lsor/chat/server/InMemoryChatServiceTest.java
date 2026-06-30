package br.edu.ifal.lsor.chat.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryChatServiceTest {

  private final InMemoryChatService service = new InMemoryChatService();

  @Test
  void rejectsNonLoginBeforeAuthentication() {
    ServiceResult result = service.handle(new ChatSession(), request(Actions.HEARTBEAT));

    assertEquals("ERROR", result.response().status());
    assertEquals(Codes.AUTH_REQUIRED, result.response().code());
  }

  @Test
  void rejectsInvalidProtocolVersion() {
    ClientRequest request =
        new ClientRequest(
            "0.9", UUID.randomUUID(), Actions.LOGIN, java.time.Instant.now(), Map.of());

    ServiceResult result = service.handle(new ChatSession(), request);

    assertEquals(Codes.INVALID_PAYLOAD, result.response().code());
  }

  @Test
  void rejectsSecondLoginOnSameSession() {
    ChatSession session = new ChatSession();
    login(session, "maria");

    ServiceResult result = login(session, "joao");

    assertEquals(Codes.INVALID_PAYLOAD, result.response().code());
  }

  @Test
  void loginNormalizesUsernameAndRejectsOnlineDuplicateIgnoringCase() {
    ChatSession first = new ChatSession();
    ChatSession second = new ChatSession();

    ServiceResult accepted =
        service.handle(
            first,
            request(
                Actions.LOGIN,
                Map.of(
                    "username", "Maria",
                    "displayName", "Maria Silva")));
    ServiceResult duplicate =
        service.handle(
            second,
            request(
                Actions.LOGIN,
                Map.of(
                    "username", "maria",
                    "displayName", "Outra Maria")));

    assertEquals(Codes.LOGIN_ACCEPTED, accepted.response().code());
    assertEquals("maria", first.username());
    assertEquals("maria", accepted.response().payload().get("username"));
    assertEquals("Maria Silva", accepted.response().payload().get("displayName"));
    assertEquals(Codes.USERNAME_ALREADY_ONLINE, duplicate.response().code());
  }

  @Test
  void loginCreatesSessionAndEmitsPresenceToOtherUsers() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");

    ServiceResult result = login(joao, "joao");

    assertEquals(Codes.LOGIN_ACCEPTED, result.response().code());
    assertEquals("joao", joao.username());
    assertEquals(1, result.events().size());
    assertEquals(Events.USER_ONLINE, result.events().get(0).event().eventType());
    assertEquals(List.of("maria"), List.copyOf(result.events().get(0).targetUsernames()));
  }

  @Test
  void directMessageRequiresOnlineTargetAndReturnsHistory() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");
    login(joao, "joao");

    ServiceResult sent =
        service.handle(
            maria,
            request(
                Actions.SEND_DIRECT,
                Map.of(
                    "targetUsername", "joao",
                    "text", "Oi")));
    ServiceResult history =
        service.handle(
            joao,
            request(
                Actions.GET_HISTORY,
                Map.of(
                    "scope", "DIRECT",
                    "target", "maria")));

    assertEquals(Codes.MESSAGE_ACCEPTED, sent.response().code());
    assertEquals(Events.DIRECT_MESSAGE, sent.events().get(0).event().eventType());
    assertEquals(Codes.HISTORY_RETURNED, history.response().code());
    List<?> messages = (List<?>) history.response().payload().get("messages");
    assertEquals(1, messages.size());
  }

  @Test
  void directMessageTargetIsCaseInsensitive() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");
    login(joao, "joao");

    ServiceResult result =
        service.handle(
            joao,
            request(
                Actions.SEND_DIRECT,
                Map.of(
                    "targetUsername", "MARIA",
                    "text", "Oi")));

    assertEquals(Codes.MESSAGE_ACCEPTED, result.response().code());
  }

  @Test
  void directMessageToOfflineExistingUserIsAcceptedAndDeliveredOnLogin() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");
    login(joao, "joao");
    service.handle(joao, request(Actions.LOGOUT));

    ServiceResult result =
        service.handle(
            maria,
            request(
                Actions.SEND_DIRECT,
                Map.of(
                    "targetUsername", "joao",
                    "text", "Oi")));

    assertEquals(Codes.MESSAGE_ACCEPTED, result.response().code());
    assertEquals(Boolean.FALSE, result.response().payload().get("deliveredToOnline"));
    assertTrue(result.events().isEmpty());

    ChatSession reconnectedJoao = new ChatSession();
    ServiceResult loginResult = login(reconnectedJoao, "joao");
    List<OutboundEvent> directEvents = directEvents(loginResult);

    assertEquals(1, directEvents.size());
    assertEquals(List.of("joao"), List.copyOf(directEvents.get(0).targetUsernames()));
    assertEquals(Events.DIRECT_MESSAGE, directEvents.get(0).event().eventType());
    assertEquals("maria", directEvents.get(0).event().payload().get("fromUsername"));
    assertEquals("Oi", directEvents.get(0).event().payload().get("text"));
    assertEquals(
        result.response().payload().get("messageId"),
        directEvents.get(0).event().payload().get("messageId"));
  }

  @Test
  void offlineDirectMessagesAreDrainedOnceInSendOrder() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");
    login(joao, "joao");
    service.handle(joao, request(Actions.LOGOUT));

    service.handle(
        maria, request(Actions.SEND_DIRECT, Map.of("targetUsername", "joao", "text", "um")));
    service.handle(
        maria, request(Actions.SEND_DIRECT, Map.of("targetUsername", "joao", "text", "dois")));

    ChatSession reconnectedJoao = new ChatSession();
    ServiceResult firstLogin = login(reconnectedJoao, "joao");
    List<OutboundEvent> firstDelivery = directEvents(firstLogin);

    assertEquals(2, firstDelivery.size());
    assertEquals(
        List.of("um", "dois"),
        firstDelivery.stream().map(event -> event.event().payload().get("text")).toList());

    service.handle(reconnectedJoao, request(Actions.LOGOUT));
    ChatSession joaoAgain = new ChatSession();
    ServiceResult secondLogin = login(joaoAgain, "joao");

    assertTrue(directEvents(secondLogin).isEmpty());
  }

  @Test
  void directMessageToOfflineUserIsReturnedInHistory() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");
    login(joao, "joao");
    service.handle(joao, request(Actions.LOGOUT));

    service.handle(
        maria,
        request(
            Actions.SEND_DIRECT,
            Map.of(
                "targetUsername", "joao",
                "text", "Oi offline")));

    ServerResponse response =
        service
            .handle(
                maria,
                request(
                    Actions.GET_HISTORY,
                    Map.of(
                        "scope", "DIRECT",
                        "target", "joao")))
            .response();

    List<?> messages = (List<?>) response.payload().get("messages");
    assertEquals(Codes.HISTORY_RETURNED, response.code());
    assertEquals(1, messages.size());
    Map<?, ?> message = (Map<?, ?>) messages.get(0);
    assertEquals("maria", message.get("fromUsername"));
    assertEquals("joao", message.get("toUsername"));
    assertEquals("Oi offline", message.get("text"));
  }

  @Test
  void directMessageToUnknownUserReturnsUserNotFound() {
    ChatSession maria = new ChatSession();
    login(maria, "maria");

    ServiceResult result =
        service.handle(
            maria,
            request(
                Actions.SEND_DIRECT,
                Map.of(
                    "targetUsername", "joao",
                    "text", "Oi")));

    assertEquals(Codes.USER_NOT_FOUND, result.response().code());
    assertTrue(result.events().isEmpty());
  }

  @Test
  void groupLifecycleHonorsOwnerAndMembershipRules() {
    ChatSession owner = new ChatSession();
    ChatSession guest = new ChatSession();
    login(owner, "owner");
    login(guest, "guest");

    assertEquals(
        Codes.GROUP_CREATED,
        service
            .handle(
                owner,
                request(
                    Actions.CREATE_GROUP,
                    Map.of(
                        "groupCode", "devs",
                        "displayName", "Desenvolvedores")))
            .response()
            .code());
    assertEquals(
        Codes.GROUP_JOINED,
        service
            .handle(guest, request(Actions.JOIN_GROUP, Map.of("groupCode", "devs")))
            .response()
            .code());
    assertEquals(
        Codes.OWNER_CANNOT_LEAVE,
        service
            .handle(owner, request(Actions.LEAVE_GROUP, Map.of("groupCode", "devs")))
            .response()
            .code());
    assertEquals(
        Codes.PERMISSION_DENIED,
        service
            .handle(
                guest,
                request(
                    Actions.RENAME_GROUP,
                    Map.of(
                        "groupCode", "devs",
                        "displayName", "Novo nome")))
            .response()
            .code());
    assertEquals(
        Codes.GROUP_RENAMED,
        service
            .handle(
                owner,
                request(
                    Actions.RENAME_GROUP,
                    Map.of(
                        "groupCode", "devs",
                        "displayName", "Novo nome")))
            .response()
            .code());
  }

  @Test
  void listGroupsIncludesMemberUsernamesInJoinOrder() {
    ChatSession owner = new ChatSession();
    ChatSession guest = new ChatSession();
    login(owner, "owner");
    login(guest, "guest");
    service.handle(
        owner,
        request(
            Actions.CREATE_GROUP,
            Map.of(
                "groupCode", "devs",
                "displayName", "Devs")));
    service.handle(guest, request(Actions.JOIN_GROUP, Map.of("groupCode", "devs")));

    ServerResponse response = service.handle(guest, request(Actions.LIST_GROUPS)).response();

    List<?> groups = (List<?>) response.payload().get("groups");
    Map<?, ?> devs =
        groups.stream()
            .map(Map.class::cast)
            .filter(group -> "devs".equals(group.get("groupCode")))
            .findFirst()
            .orElseThrow();
    assertEquals(Codes.GROUPS_LISTED, response.code());
    assertEquals(2, devs.get("memberCount"));
    assertEquals(Boolean.TRUE, devs.get("isMember"));
    assertEquals(List.of("owner", "guest"), devs.get("memberUsernames"));
  }

  @Test
  void groupHistoryIsBlockedForNonMembers() {
    ChatSession owner = new ChatSession();
    ChatSession outsider = new ChatSession();
    login(owner, "owner");
    login(outsider, "outsider");
    service.handle(
        owner,
        request(
            Actions.CREATE_GROUP,
            Map.of(
                "groupCode", "devs",
                "displayName", "Devs")));

    ServiceResult result =
        service.handle(
            outsider,
            request(
                Actions.GET_HISTORY,
                Map.of(
                    "scope", "GROUP",
                    "target", "devs")));

    assertEquals(Codes.NOT_GROUP_MEMBER, result.response().code());
  }

  @Test
  void historyLimitIsCappedAtOneHundred() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");
    login(joao, "joao");
    for (int index = 0; index < 120; index++) {
      service.handle(
          maria,
          request(Actions.SEND_DIRECT, Map.of("targetUsername", "joao", "text", "msg " + index)));
    }

    ServerResponse response =
        service
            .handle(
                maria,
                request(
                    Actions.GET_HISTORY,
                    Map.of(
                        "scope", "DIRECT",
                        "target", "joao",
                        "limit", 120)))
            .response();

    List<?> messages = (List<?>) response.payload().get("messages");
    assertEquals(100, messages.size());
  }

  @Test
  void historyRejectsZeroLimit() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");
    login(joao, "joao");

    ServiceResult result =
        service.handle(
            maria,
            request(
                Actions.GET_HISTORY,
                Map.of(
                    "scope", "DIRECT",
                    "target", "joao",
                    "limit", 0)));

    assertEquals(Codes.INVALID_PAYLOAD, result.response().code());
  }

  @Test
  void historyRejectsStringLimit() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");
    login(joao, "joao");

    ServiceResult result =
        service.handle(
            maria,
            request(
                Actions.GET_HISTORY,
                Map.of(
                    "scope", "DIRECT",
                    "target", "joao",
                    "limit", "10")));

    assertEquals(Codes.INVALID_PAYLOAD, result.response().code());
  }

  @Test
  void historyRejectsTargetLongerThanScopeAllows() {
    ChatSession maria = new ChatSession();
    login(maria, "maria");
    String longTarget = "a".repeat(PayloadLimits.MAX_USERNAME_LENGTH + 1);

    ServiceResult result =
        service.handle(
            maria, request(Actions.GET_HISTORY, Map.of("scope", "DIRECT", "target", longTarget)));

    assertEquals(Codes.INVALID_PAYLOAD, result.response().code());
  }

  @Test
  void listGroupsRejectsStringOnlyMine() {
    ChatSession maria = new ChatSession();
    login(maria, "maria");

    ServiceResult result =
        service.handle(maria, request(Actions.LIST_GROUPS, Map.of("onlyMine", "true")));

    assertEquals(Codes.INVALID_PAYLOAD, result.response().code());
  }

  @Test
  void listGroupsDefaultsToCurrentUserMembershipOnly() {
    ChatSession owner = new ChatSession();
    ChatSession outsider = new ChatSession();
    login(owner, "owner");
    login(outsider, "outsider");
    service.handle(
        owner,
        request(
            Actions.CREATE_GROUP,
            Map.of(
                "groupCode", "devs",
                "displayName", "Devs")));

    ServiceResult result = service.handle(outsider, request(Actions.LIST_GROUPS));

    List<?> groups = (List<?>) result.response().payload().get("groups");
    assertEquals(Codes.GROUPS_LISTED, result.response().code());
    assertTrue(groups.isEmpty());
  }

  @Test
  void listGroupsCanStillReturnAllGroupsWhenRequested() {
    ChatSession owner = new ChatSession();
    ChatSession outsider = new ChatSession();
    login(owner, "owner");
    login(outsider, "outsider");
    service.handle(
        owner,
        request(
            Actions.CREATE_GROUP,
            Map.of(
                "groupCode", "devs",
                "displayName", "Devs")));

    ServiceResult result =
        service.handle(outsider, request(Actions.LIST_GROUPS, Map.of("onlyMine", false)));

    List<?> groups = (List<?>) result.response().payload().get("groups");
    Map<?, ?> group = (Map<?, ?>) groups.get(0);
    assertEquals(Codes.GROUPS_LISTED, result.response().code());
    assertEquals(1, groups.size());
    assertFalse((Boolean) group.get("isMember"));
  }

  @Test
  void sendGroupRejectsMissingPayload() {
    ChatSession maria = new ChatSession();
    login(maria, "maria");

    ServiceResult result = service.handle(maria, request(Actions.SEND_GROUP));

    assertEquals(Codes.INVALID_PAYLOAD, result.response().code());
  }

  @Test
  void loginRejectsUsernameLongerThanMaxLength() {
    ChatSession session = new ChatSession();
    String longUsername = "a".repeat(PayloadLimits.MAX_USERNAME_LENGTH + 1);

    ServiceResult result =
        service.handle(
            session,
            request(Actions.LOGIN, Map.of("username", longUsername, "displayName", "Valid")));

    assertEquals(Codes.INVALID_PAYLOAD, result.response().code());
  }

  @Test
  void sendDirectRejectsTextLongerThanMaxLength() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");
    login(joao, "joao");
    String longText = "a".repeat(PayloadLimits.MAX_MESSAGE_TEXT_LENGTH + 1);

    ServiceResult result =
        service.handle(
            maria,
            request(Actions.SEND_DIRECT, Map.of("targetUsername", "joao", "text", longText)));

    assertEquals(Codes.INVALID_PAYLOAD, result.response().code());
  }

  @Test
  void directHistoryRetainsOnlyMaxHistoryMessages() {
    ChatSession maria = new ChatSession();
    ChatSession joao = new ChatSession();
    login(maria, "maria");
    login(joao, "joao");

    for (int index = 0; index < PayloadLimits.MAX_HISTORY_MESSAGES + 1; index++) {
      service.handle(
          maria,
          request(Actions.SEND_DIRECT, Map.of("targetUsername", "joao", "text", "msg " + index)));
    }

    ServerResponse response =
        service
            .handle(
                maria,
                request(
                    Actions.GET_HISTORY,
                    Map.of(
                        "scope", "DIRECT",
                        "target", "joao",
                        "limit", 100)))
            .response();

    List<?> messages = (List<?>) response.payload().get("messages");
    assertEquals(100, messages.size());
  }

  @Test
  void groupHistoryRetainsOnlyMaxHistoryMessages() {
    ChatSession owner = new ChatSession();
    login(owner, "owner");
    service.handle(
        owner,
        request(
            Actions.CREATE_GROUP,
            Map.of(
                "groupCode", "devs",
                "displayName", "Devs")));

    for (int index = 0; index < PayloadLimits.MAX_HISTORY_MESSAGES + 1; index++) {
      service.handle(
          owner, request(Actions.SEND_GROUP, Map.of("groupCode", "devs", "text", "msg " + index)));
    }

    ServerResponse response =
        service
            .handle(
                owner,
                request(
                    Actions.GET_HISTORY,
                    Map.of(
                        "scope", "GROUP",
                        "target", "devs",
                        "limit", 100)))
            .response();

    List<?> messages = (List<?>) response.payload().get("messages");
    assertEquals(100, messages.size());
  }

  private ServiceResult login(ChatSession session, String username) {
    return service.handle(
        session,
        request(
            Actions.LOGIN,
            Map.of(
                "username", username,
                "displayName", username)));
  }

  private ClientRequest request(String action) {
    return request(action, Map.of());
  }

  private ClientRequest request(String action, Map<String, ? extends Serializable> payload) {
    return new ClientRequest(
        "1.0", UUID.randomUUID(), action, java.time.Instant.now(), Map.copyOf(payload));
  }

  private static List<OutboundEvent> directEvents(ServiceResult result) {
    return result.events().stream()
        .filter(event -> Events.DIRECT_MESSAGE.equals(event.event().eventType()))
        .toList();
  }
}
