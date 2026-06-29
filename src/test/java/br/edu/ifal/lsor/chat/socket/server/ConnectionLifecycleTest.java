package br.edu.ifal.lsor.chat.socket.server;

import static org.junit.jupiter.api.Assertions.*;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.server.ChatSession;
import br.edu.ifal.lsor.chat.server.InMemoryChatService;
import br.edu.ifal.lsor.chat.server.ServiceResult;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConnectionLifecycleTest {

  private InMemoryChatService service;
  private ConnectionRegistry registry;
  private ConnectionLifecycle lifecycle;

  @BeforeEach
  void setUp() {
    service = new InMemoryChatService();
    registry = new ConnectionRegistry();
    lifecycle = new ConnectionLifecycle(service, registry);
  }

  private static ClientConnection createConnection() {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    try {
      ObjectOutputStream out = new ObjectOutputStream(byteStream);
      return new ClientConnection(out);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  private static ClientRequest loginRequest(String username) {
    return ClientRequest.of(Actions.LOGIN, Map.of("username", username, "displayName", username));
  }

  @Test
  void loginRegistersConnection() {
    ChatSession session = new ChatSession();
    ClientConnection connection = createConnection();

    ServiceResult result = lifecycle.handle(session, connection, loginRequest("alice"));

    assertEquals(Codes.LOGIN_ACCEPTED, result.response().code());
    assertFalse(result.closeConnection());
    assertNotNull(registry.connection("alice"));
    assertSame(connection, registry.connection("alice"));
  }

  @Test
  void logoutRemovesConnection() {
    ChatSession session = new ChatSession();
    ClientConnection connection = createConnection();

    lifecycle.handle(session, connection, loginRequest("alice"));
    assertNotNull(registry.connection("alice"));

    ServiceResult result =
        lifecycle.handle(session, connection, ClientRequest.of(Actions.LOGOUT, Map.of()));

    assertEquals(Codes.LOGOUT_ACCEPTED, result.response().code());
    assertTrue(result.closeConnection());
    assertNull(registry.connection("alice"));
  }

  @Test
  void disconnectRemovesConnection() {
    ChatSession session = new ChatSession();
    ClientConnection connection = createConnection();

    lifecycle.handle(session, connection, loginRequest("alice"));
    assertNotNull(registry.connection("alice"));

    lifecycle.disconnect(session);

    assertNull(registry.connection("alice"));
  }
}
