package br.edu.ifal.lsor.chat.socket.server;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.server.ChatSession;
import br.edu.ifal.lsor.chat.server.InMemoryChatService;
import br.edu.ifal.lsor.chat.server.OutboundEvent;
import br.edu.ifal.lsor.chat.server.ServiceResult;
import java.util.List;

final class ConnectionLifecycle {

  private final InMemoryChatService service;
  private final ConnectionRegistry connections;

  ConnectionLifecycle(InMemoryChatService service, ConnectionRegistry connections) {
    this.service = service;
    this.connections = connections;
  }

  synchronized ServiceResult handle(
      ChatSession session, ClientConnection connection, ClientRequest request) {
    String previousUsername = session.username();
    boolean wasAuthenticated = session.isAuthenticated();
    ServiceResult result = service.handle(session, request);
    if (!wasAuthenticated && session.isAuthenticated()) {
      connections.register(session, connection);
    }
    if (result.closeConnection() && previousUsername != null) {
      connections.remove(previousUsername);
    }
    return result;
  }

  synchronized List<OutboundEvent> disconnect(ChatSession session) {
    String username = session.username();
    if (username != null) {
      connections.remove(username);
    }
    return service.disconnect(session);
  }
}
