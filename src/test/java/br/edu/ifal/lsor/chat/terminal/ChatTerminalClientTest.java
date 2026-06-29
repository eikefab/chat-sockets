package br.edu.ifal.lsor.chat.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import br.edu.ifal.lsor.chat.server.InMemoryChatService;
import br.edu.ifal.lsor.chat.socket.client.ChatClientSocket;
import br.edu.ifal.lsor.chat.socket.server.ChatProtocolSocketHandler;
import br.edu.ifal.lsor.chat.socket.server.ChatServer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ChatTerminalClientTest {

  private record TestServerFixture(ChatServer server, Thread serverThread) {}

  private static TestServerFixture startTestServer(int maxClients) throws Exception {
    ChatProtocolSocketHandler handler = new ChatProtocolSocketHandler(new InMemoryChatService());
    ChatServer server = new ChatServer("127.0.0.1", 0, maxClients, handler::handle);
    Thread serverThread = new Thread(server::initServer, "chat-test-server");
    serverThread.setDaemon(true);
    serverThread.start();
    assertTrue(server.awaitStarted(3, TimeUnit.SECONDS));
    return new TestServerFixture(server, serverThread);
  }

  private static void stopTestServer(TestServerFixture fixture) throws Exception {
    fixture.server.stopServer();
    fixture.serverThread.join(1000);
  }

  private static List<ServerEvent> drainAll(BlockingQueue<ServerEvent> queue) {
    List<ServerEvent> drained = new ArrayList<>();
    ServerEvent event = queue.poll();
    while (event != null) {
      drained.add(event);
      event = queue.poll();
    }
    return drained;
  }

  @Test
  void listFromPayloadExtractsListSafely() {
    Map<String, Serializable> payload =
        Map.of("items", (Serializable) List.of(Map.of("key", "value")));

    List<Map<String, Serializable>> result = ChatTerminalClient.listFromPayload(payload, "items");

    assertEquals(1, result.size());
    assertEquals("value", result.get(0).get("key"));
  }

  @Test
  void listFromPayloadReturnsEmptyWhenKeyMissing() {
    Map<String, Serializable> payload = Map.of();

    List<Map<String, Serializable>> result = ChatTerminalClient.listFromPayload(payload, "other");

    assertTrue(result.isEmpty());
  }

  @Test
  void listFromPayloadReturnsEmptyWhenValueIsNotList() {
    Map<String, Serializable> payload = Map.of("items", "not-a-list");

    List<Map<String, Serializable>> result = ChatTerminalClient.listFromPayload(payload, "items");

    assertTrue(result.isEmpty());
  }

  @Test
  void socketCorrelatesLoginAndDirectMessage() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    BlockingQueue<ServerEvent> bobEvents = new LinkedBlockingQueue<>();

    try (ChatClientSocket alice = new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort());
        ChatClientSocket bob =
            new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort(), bobEvents::add)) {
      alice.openSocket();
      bob.openSocket();

      ServerResponse aliceLogin =
          alice
              .send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of("username", "alice-t", "displayName", "Alice"))
              .get(3, TimeUnit.SECONDS);
      ServerResponse bobLogin =
          bob.send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of("username", "bob-t", "displayName", "Bob"))
              .get(3, TimeUnit.SECONDS);

      assertEquals(Codes.LOGIN_ACCEPTED, aliceLogin.code());
      assertEquals(Codes.LOGIN_ACCEPTED, bobLogin.code());

      drainAll(bobEvents);

      ServerResponse direct =
          alice
              .send(
                  Actions.SEND_DIRECT,
                  Map.<String, Serializable>of("targetUsername", "bob-t", "text", "Hello"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.MESSAGE_ACCEPTED, direct.code());

      ServerEvent event = bobEvents.poll(3, TimeUnit.SECONDS);
      assertNotNull(event);
      assertEquals(Events.DIRECT_MESSAGE, event.eventType());
      assertEquals("alice-t", event.payload().get("fromUsername"));
      assertEquals("Hello", event.payload().get("text"));
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void groupMessageHistoryIsReturnedForMember() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    try (ChatClientSocket owner = new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort());
        ChatClientSocket member =
            new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort())) {
      owner.openSocket();
      member.openSocket();

      owner
          .send(
              Actions.LOGIN,
              Map.<String, Serializable>of("username", "owner-t", "displayName", "Owner"))
          .get(3, TimeUnit.SECONDS);
      member
          .send(
              Actions.LOGIN,
              Map.<String, Serializable>of("username", "member-t", "displayName", "Member"))
          .get(3, TimeUnit.SECONDS);

      owner
          .send(
              Actions.CREATE_GROUP,
              Map.<String, Serializable>of("groupCode", "test-g", "displayName", "Test Group"))
          .get(3, TimeUnit.SECONDS);
      member
          .send(Actions.JOIN_GROUP, Map.<String, Serializable>of("groupCode", "test-g"))
          .get(3, TimeUnit.SECONDS);

      owner
          .send(
              Actions.SEND_GROUP,
              Map.<String, Serializable>of("groupCode", "test-g", "text", "Hello group"))
          .get(3, TimeUnit.SECONDS);

      ServerResponse history =
          member
              .send(
                  Actions.GET_HISTORY,
                  Map.<String, Serializable>of("scope", "GROUP", "target", "test-g"))
              .get(3, TimeUnit.SECONDS);

      assertEquals(Codes.HISTORY_RETURNED, history.code());
      assertEquals("GROUP", history.payload().get("scope"));
      assertEquals("test-g", history.payload().get("target"));
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void directMessageToOfflineUserReturnsError() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    try (ChatClientSocket alice =
        new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort())) {
      alice.openSocket();

      alice
          .send(
              Actions.LOGIN,
              Map.<String, Serializable>of("username", "alice-off", "displayName", "Alice"))
          .get(3, TimeUnit.SECONDS);

      ServerResponse response =
          alice
              .send(
                  Actions.SEND_DIRECT,
                  Map.<String, Serializable>of("targetUsername", "nonexistent", "text", "Hi"))
              .get(3, TimeUnit.SECONDS);

      assertFalse(response.isOk());
      assertEquals("USER_NOT_FOUND", response.code());
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void disconnectListenerFiresOnServerStop() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    CountDownLatch disconnected = new CountDownLatch(1);
    try (ChatClientSocket client =
        new ChatClientSocket(
            "127.0.0.1", fixture.server.getBoundPort(), event -> {}, disconnected::countDown)) {
      client.openSocket();

      client
          .send(
              Actions.LOGIN,
              Map.<String, Serializable>of("username", "dc-test", "displayName", "DC"))
          .get(3, TimeUnit.SECONDS);

      fixture.server.stopServer();
      fixture.serverThread.join(1000);

      assertTrue(disconnected.await(3, TimeUnit.SECONDS));
    }
  }

  @Test
  void refreshGroupCacheClearsCacheOnEmptyList() {
    GroupCache groupCache = new GroupCache();
    groupCache.put("devs", "Desenvolvedores");

    ServerResponse emptyGroupsResponse =
        ServerResponse.ok(
            null,
            Codes.GROUPS_LISTED,
            "Grupos listados.",
            Map.of("groups", (Serializable) List.<Map<String, Serializable>>of()));

    FakeChatClientSocket fakeClient = new FakeChatClientSocket(emptyGroupsResponse);
    CommandHandler handler =
        new CommandHandler(
            fakeClient,
            "alice",
            groupCache,
            new TerminalEventPrinter("alice", groupCache, new Object()),
            new Object());

    handler.refreshGroupCache();

    assertFalse(groupCache.contains("devs"));
  }

  private static final class FakeChatClientSocket extends ChatClientSocket {

    private final ServerResponse response;

    FakeChatClientSocket(ServerResponse response) {
      super("127.0.0.1", 0);
      this.response = response;
    }

    @Override
    public CompletableFuture<ServerResponse> send(
        String action, Map<String, Serializable> payload) {
      assertEquals(Actions.LIST_GROUPS, action);
      return CompletableFuture.completedFuture(response);
    }
  }
}
