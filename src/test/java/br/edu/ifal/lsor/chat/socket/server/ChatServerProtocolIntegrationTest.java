package br.edu.ifal.lsor.chat.socket.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import br.edu.ifal.lsor.chat.server.InMemoryChatService;
import br.edu.ifal.lsor.chat.socket.client.ChatClientSocket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ChatServerProtocolIntegrationTest {

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
  void socketProtocolCorrelatesResponsesAndDeliversEvents() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    BlockingQueue<ServerEvent> mariaEvents = new LinkedBlockingQueue<>();
    BlockingQueue<ServerEvent> joaoEvents = new LinkedBlockingQueue<>();

    try (ChatClientSocket maria =
            new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort(), mariaEvents::add);
        ChatClientSocket joao =
            new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort(), joaoEvents::add)) {
      maria.openSocket();
      joao.openSocket();

      ServerResponse mariaLogin =
          maria
              .send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of(
                      "username", "maria-it",
                      "displayName", "Maria"))
              .get(3, TimeUnit.SECONDS);
      ServerResponse joaoLogin =
          joao.send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of(
                      "username", "joao-it",
                      "displayName", "Joao"))
              .get(3, TimeUnit.SECONDS);
      ServerResponse direct =
          maria
              .send(
                  Actions.SEND_DIRECT,
                  Map.<String, Serializable>of(
                      "targetUsername", "joao-it",
                      "text", "Oi"))
              .get(3, TimeUnit.SECONDS);

      assertEquals(Codes.LOGIN_ACCEPTED, mariaLogin.code());
      assertEquals(Codes.LOGIN_ACCEPTED, joaoLogin.code());
      assertEquals(Codes.MESSAGE_ACCEPTED, direct.code());
      assertNotNull(mariaEvents.poll(3, TimeUnit.SECONDS));
      ServerEvent directEvent = joaoEvents.poll(3, TimeUnit.SECONDS);
      assertNotNull(directEvent);
      assertEquals("DIRECT_MESSAGE", directEvent.eventType());
      assertEquals("maria-it", directEvent.payload().get("fromUsername"));

      ServerResponse logout =
          maria.send(Actions.LOGOUT, Map.<String, Serializable>of()).get(3, TimeUnit.SECONDS);
      assertEquals(Codes.LOGOUT_ACCEPTED, logout.code());
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void clientSendBeforeOpenSocketThrowsIllegalStateException() {
    try (ChatClientSocket client = new ChatClientSocket("127.0.0.1", 1)) {
      assertThrows(
          IllegalStateException.class,
          () -> client.send(Actions.HEARTBEAT, Map.<String, Serializable>of()));
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }

  @Test
  void serverRejectsConnectionsAboveMaxClients() throws Exception {
    TestServerFixture fixture = startTestServer(1);

    try (ChatClientSocket accepted =
        new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort())) {
      accepted.openSocket();

      IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              () -> {
                try (ChatClientSocket rejected =
                    new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort())) {
                  rejected.openSocket();
                }
              });

      assertTrue(exception.getMessage().contains("Falha ao abrir socket"));
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void clientIsNotifiedWhenServerClosesConnection() throws Exception {
    TestServerFixture fixture = startTestServer(1);

    CountDownLatch disconnected = new CountDownLatch(1);
    try (ChatClientSocket client =
        new ChatClientSocket(
            "127.0.0.1", fixture.server.getBoundPort(), event -> {}, disconnected::countDown)) {
      client.openSocket();

      fixture.server.stopServer();
      fixture.serverThread.join(1000);

      assertTrue(disconnected.await(3, TimeUnit.SECONDS));
    }
  }

  @Test
  void presenceEventDeliveredToAlreadyConnectedUsers() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    BlockingQueue<ServerEvent> aliceEvents = new LinkedBlockingQueue<>();

    try (ChatClientSocket alice =
        new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort(), aliceEvents::add)) {
      alice.openSocket();

      ServerResponse aliceLogin =
          alice
              .send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of(
                      "username", "alice-presence",
                      "displayName", "Alice"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.LOGIN_ACCEPTED, aliceLogin.code());

      try (ChatClientSocket bob =
          new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort())) {
        bob.openSocket();

        ServerResponse bobLogin =
            bob.send(
                    Actions.LOGIN,
                    Map.<String, Serializable>of(
                        "username", "bob-presence",
                        "displayName", "Bob"))
                .get(3, TimeUnit.SECONDS);
        assertEquals(Codes.LOGIN_ACCEPTED, bobLogin.code());
      }

      ServerEvent presenceEvent = aliceEvents.poll(3, TimeUnit.SECONDS);
      assertNotNull(presenceEvent);
      assertEquals(Events.USER_ONLINE, presenceEvent.eventType());
      assertEquals("bob-presence", presenceEvent.payload().get("username"));
      assertEquals("Bob", presenceEvent.payload().get("displayName"));
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void groupMessageDeliveredToMembersButNotOutsiders() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    BlockingQueue<ServerEvent> memberEvents = new LinkedBlockingQueue<>();
    BlockingQueue<ServerEvent> outsiderEvents = new LinkedBlockingQueue<>();

    try (ChatClientSocket owner = new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort());
        ChatClientSocket member =
            new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort(), memberEvents::add);
        ChatClientSocket outsider =
            new ChatClientSocket("127.0.0.1", fixture.server.getBoundPort(), outsiderEvents::add)) {
      owner.openSocket();
      member.openSocket();
      outsider.openSocket();

      ServerResponse ownerLogin =
          owner
              .send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of(
                      "username", "group-owner",
                      "displayName", "Owner"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.LOGIN_ACCEPTED, ownerLogin.code());

      ServerResponse memberLogin =
          member
              .send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of(
                      "username", "group-member",
                      "displayName", "Member"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.LOGIN_ACCEPTED, memberLogin.code());

      ServerResponse outsiderLogin =
          outsider
              .send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of(
                      "username", "group-outsider",
                      "displayName", "Outsider"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.LOGIN_ACCEPTED, outsiderLogin.code());

      ServerResponse createGroup =
          owner
              .send(
                  Actions.CREATE_GROUP,
                  Map.<String, Serializable>of(
                      "groupCode", "devs",
                      "displayName", "Devs Group"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.GROUP_CREATED, createGroup.code());

      ServerResponse joinGroup =
          member
              .send(Actions.JOIN_GROUP, Map.<String, Serializable>of("groupCode", "devs"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.GROUP_JOINED, joinGroup.code());

      drainAll(outsiderEvents);

      ServerResponse sendGroup =
          owner
              .send(
                  Actions.SEND_GROUP,
                  Map.<String, Serializable>of(
                      "groupCode", "devs",
                      "text", "Hello group"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.MESSAGE_ACCEPTED, sendGroup.code());

      drainAll(memberEvents);
      ServerEvent memberMessage = memberEvents.poll(1, TimeUnit.SECONDS);
      if (memberMessage != null && !Events.GROUP_MESSAGE.equals(memberMessage.eventType())) {
        memberEvents.add(memberMessage);
        memberMessage = memberEvents.poll(1, TimeUnit.SECONDS);
      }
      assertNotNull(memberMessage);
      assertEquals(Events.GROUP_MESSAGE, memberMessage.eventType());
      assertEquals("devs", memberMessage.payload().get("groupCode"));
      assertEquals("group-owner", memberMessage.payload().get("authorUsername"));

      List<ServerEvent> outsiderRemaining = drainAll(outsiderEvents);
      boolean outsiderGotGroupMessage =
          outsiderRemaining.stream().anyMatch(e -> Events.GROUP_MESSAGE.equals(e.eventType()));
      assertTrue(!outsiderGotGroupMessage, "outsider should not receive GROUP_MESSAGE");
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void sendFailsAfterServerDisconnects() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    CountDownLatch disconnected = new CountDownLatch(1);
    try (ChatClientSocket client =
        new ChatClientSocket(
            "127.0.0.1", fixture.server.getBoundPort(), event -> {}, disconnected::countDown)) {
      client.openSocket();

      ServerResponse login =
          client
              .send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of(
                      "username", "disconnect-test",
                      "displayName", "Test"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.LOGIN_ACCEPTED, login.code());

      fixture.server.stopServer();
      fixture.serverThread.join(1000);

      assertTrue(disconnected.await(3, TimeUnit.SECONDS));

      assertThrows(
          IllegalStateException.class,
          () -> client.send(Actions.HEARTBEAT, Map.<String, Serializable>of()));
    }
  }

  @Test
  void defaultConstructorHasBoundedMaxClients() {
    ChatProtocolSocketHandler handler = new ChatProtocolSocketHandler(new InMemoryChatService());
    ChatServer server = new ChatServer(0, handler::handle);
    assertTrue(server.maxClients() > 0);
    assertTrue(server.maxClients() < Integer.MAX_VALUE);
  }

  @Test
  void directMessageReachesJustLoggedInUser() throws Exception {
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
                  Map.<String, Serializable>of(
                      "username", "alice-atomic",
                      "displayName", "Alice"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.LOGIN_ACCEPTED, aliceLogin.code());

      ServerResponse bobLogin =
          bob.send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of(
                      "username", "bob-atomic",
                      "displayName", "Bob"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.LOGIN_ACCEPTED, bobLogin.code());

      drainAll(bobEvents);

      ServerResponse direct =
          alice
              .send(
                  Actions.SEND_DIRECT,
                  Map.<String, Serializable>of(
                      "targetUsername", "bob-atomic",
                      "text", "Hello Bob"))
              .get(3, TimeUnit.SECONDS);
      assertEquals(Codes.MESSAGE_ACCEPTED, direct.code());

      ServerEvent directEvent = bobEvents.poll(3, TimeUnit.SECONDS);
      assertNotNull(directEvent);
      assertEquals(Events.DIRECT_MESSAGE, directEvent.eventType());
      assertEquals("alice-atomic", directEvent.payload().get("fromUsername"));
    } finally {
      stopTestServer(fixture);
    }
  }

  @Test
  void nonProtocolClassIsRejectedByInputFilter() throws Exception {
    TestServerFixture fixture = startTestServer(10);

    try (Socket rawSocket = new Socket("127.0.0.1", fixture.server.getBoundPort())) {
      rawSocket.setSoTimeout(3000);
      ObjectOutputStream out = new ObjectOutputStream(rawSocket.getOutputStream());
      out.flush();

      Thread.sleep(100);

      ObjectInputStream in = new ObjectInputStream(rawSocket.getInputStream());
      out.writeObject(BigInteger.valueOf(42));
      out.flush();

      assertThrows(Exception.class, in::readObject);
    } finally {
      stopTestServer(fixture);
    }
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
}
