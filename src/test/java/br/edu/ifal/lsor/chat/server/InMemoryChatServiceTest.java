package br.edu.ifal.lsor.chat.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
  void directMessageToOfflineExistingUserReturnsUserOffline() {
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

    assertEquals(Codes.USER_OFFLINE, result.response().code());
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
}
