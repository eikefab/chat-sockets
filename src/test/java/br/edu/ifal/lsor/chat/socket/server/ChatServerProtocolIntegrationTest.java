package br.edu.ifal.lsor.chat.socket.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import br.edu.ifal.lsor.chat.server.InMemoryChatService;
import br.edu.ifal.lsor.chat.socket.client.ChatClientSocket;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ChatServerProtocolIntegrationTest {

  @Test
  void socketProtocolCorrelatesResponsesAndDeliversEvents() throws Exception {
    ChatProtocolSocketHandler handler = new ChatProtocolSocketHandler(new InMemoryChatService());
    ChatServer server = new ChatServer("127.0.0.1", 0, 10, handler::handle);
    Thread serverThread = new Thread(server::initServer, "chat-test-server");
    serverThread.setDaemon(true);
    serverThread.start();
    assertTrue(server.awaitStarted(3, TimeUnit.SECONDS));

    BlockingQueue<ServerEvent> mariaEvents = new LinkedBlockingQueue<>();
    BlockingQueue<ServerEvent> joaoEvents = new LinkedBlockingQueue<>();

    try (ChatClientSocket maria =
            new ChatClientSocket("127.0.0.1", server.getBoundPort(), mariaEvents::add);
        ChatClientSocket joao =
            new ChatClientSocket("127.0.0.1", server.getBoundPort(), joaoEvents::add)) {
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
      server.stopServer();
      serverThread.join(1000);
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
    ChatProtocolSocketHandler handler = new ChatProtocolSocketHandler(new InMemoryChatService());
    ChatServer server = new ChatServer("127.0.0.1", 0, 1, handler::handle);
    Thread serverThread = new Thread(server::initServer, "chat-test-server-limit");
    serverThread.setDaemon(true);
    serverThread.start();
    assertTrue(server.awaitStarted(3, TimeUnit.SECONDS));

    try (ChatClientSocket accepted = new ChatClientSocket("127.0.0.1", server.getBoundPort())) {
      accepted.openSocket();

      IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              () -> {
                try (ChatClientSocket rejected =
                    new ChatClientSocket("127.0.0.1", server.getBoundPort())) {
                  rejected.openSocket();
                }
              });

      assertTrue(exception.getMessage().contains("Falha ao abrir socket"));
    } finally {
      server.stopServer();
      serverThread.join(1000);
    }
  }

  @Test
  void clientIsNotifiedWhenServerClosesConnection() throws Exception {
    ChatProtocolSocketHandler handler = new ChatProtocolSocketHandler(new InMemoryChatService());
    ChatServer server = new ChatServer("127.0.0.1", 0, 1, handler::handle);
    Thread serverThread = new Thread(server::initServer, "chat-test-server-disconnect");
    serverThread.setDaemon(true);
    serverThread.start();
    assertTrue(server.awaitStarted(3, TimeUnit.SECONDS));

    CountDownLatch disconnected = new CountDownLatch(1);
    try (ChatClientSocket client =
        new ChatClientSocket(
            "127.0.0.1", server.getBoundPort(), event -> {}, disconnected::countDown)) {
      client.openSocket();

      server.stopServer();
      serverThread.join(1000);

      assertTrue(disconnected.await(3, TimeUnit.SECONDS));
    }
  }
}
