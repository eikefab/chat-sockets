package br.edu.ifal.lsor.chat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.server.InMemoryChatService;
import br.edu.ifal.lsor.chat.socket.server.ChatProtocolSocketHandler;
import br.edu.ifal.lsor.chat.socket.server.ChatServer;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ChatClientSessionTest {

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

  @Test
  void socketCorrelatesLoginAndDirectMessage() throws Exception {
    TestServerFixture fixture = startTestServer(10);
    BlockingQueue<ChatMessage> bobMessages = new LinkedBlockingQueue<>();

    try (ChatClientSession alice =
            new ChatClientSession(
                "127.0.0.1", fixture.server.getBoundPort(), new ChatClientListener() {});
        ChatClientSession bob =
            new ChatClientSession(
                "127.0.0.1",
                fixture.server.getBoundPort(),
                new ChatClientListener() {
                  @Override
                  public void onDirectMessage(ChatMessage message) {
                    bobMessages.add(message);
                  }
                })) {
      alice.connect();
      bob.connect();
      assertEquals(
          Codes.LOGIN_ACCEPTED, alice.login("alice-t", "Alice").get(3, TimeUnit.SECONDS).code());
      assertEquals(Codes.LOGIN_ACCEPTED, bob.login("bob-t", "Bob").get(3, TimeUnit.SECONDS).code());

      ConversationTarget bobTarget =
          new ConversationTarget(ConversationKind.DIRECT, "bob-t", "Bob (bob-t)");
      assertEquals(
          Codes.MESSAGE_ACCEPTED,
          alice.sendMessage(bobTarget, "Hello").get(3, TimeUnit.SECONDS).code());

      ChatMessage message = bobMessages.poll(3, TimeUnit.SECONDS);
      assertNotNull(message);
      assertEquals(ConversationKind.DIRECT, message.kind());
      assertEquals("alice-t", message.authorUsername());
      assertEquals("Hello", message.text());
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void groupMessageHistoryIsReturnedForMember() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    try (ChatClientSession owner =
            new ChatClientSession(
                "127.0.0.1", fixture.server.getBoundPort(), new ChatClientListener() {});
        ChatClientSession member =
            new ChatClientSession(
                "127.0.0.1", fixture.server.getBoundPort(), new ChatClientListener() {})) {
      owner.connect();
      member.connect();
      owner.login("owner-t", "Owner").get(3, TimeUnit.SECONDS);
      member.login("member-t", "Member").get(3, TimeUnit.SECONDS);

      owner.createGroup("test-g", "Test Group").get(3, TimeUnit.SECONDS);
      member.joinGroup("test-g").get(3, TimeUnit.SECONDS);
      ConversationTarget group =
          new ConversationTarget(ConversationKind.GROUP, "test-g", "#test-g - Test Group");
      owner.sendMessage(group, "Hello group").get(3, TimeUnit.SECONDS);

      List<ChatMessage> history = member.history(group).get(3, TimeUnit.SECONDS);

      assertFalse(history.isEmpty());
      assertEquals(ConversationKind.GROUP, history.get(0).kind());
      assertEquals("test-g", history.get(0).conversationId());
      assertEquals("Hello group", history.get(0).text());
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void directMessageToOfflineUserReturnsError() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    try (ChatClientSession alice =
        new ChatClientSession(
            "127.0.0.1", fixture.server.getBoundPort(), new ChatClientListener() {})) {
      alice.connect();
      alice.login("alice-off", "Alice").get(3, TimeUnit.SECONDS);

      ConversationTarget missing =
          new ConversationTarget(ConversationKind.DIRECT, "nonexistent", "nonexistent");

      assertEquals(
          Codes.USER_NOT_FOUND, alice.sendMessage(missing, "Hi").get(3, TimeUnit.SECONDS).code());
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void disconnectListenerFiresOnServerStop() throws Exception {
    TestServerFixture fixture = startTestServer(10);
    CountDownLatch disconnected = new CountDownLatch(1);

    try (ChatClientSession client =
        new ChatClientSession(
            "127.0.0.1",
            fixture.server.getBoundPort(),
            new ChatClientListener() {
              @Override
              public void onDisconnected() {
                disconnected.countDown();
              }
            })) {
      client.connect();
      client.login("dc-test", "DC").get(3, TimeUnit.SECONDS);

      fixture.server.stopServer();
      fixture.serverThread.join(1000);

      assertTrue(disconnected.await(3, TimeUnit.SECONDS));
    }
  }
}
