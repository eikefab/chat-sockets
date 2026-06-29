package br.edu.ifal.lsor.chat.socket.server;

import br.edu.ifal.lsor.chat.server.ChatSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class ConnectionRegistry {

  private final ConcurrentMap<String, RegisteredConnection> connections = new ConcurrentHashMap<>();

  void register(ChatSession session, ClientConnection connection) {
    connections.put(session.username(), new RegisteredConnection(connection, session));
  }

  ClientConnection connection(String username) {
    RegisteredConnection registered = connections.get(username);
    return registered == null ? null : registered.connection();
  }

  ChatSession remove(String username) {
    RegisteredConnection registered = connections.remove(username);
    return registered == null ? null : registered.session();
  }

  ChatSession remove(String username, ClientConnection connection) {
    RegisteredConnection registered = connections.get(username);
    if (registered == null || registered.connection() != connection) {
      return null;
    }
    return connections.remove(username, registered) ? registered.session() : null;
  }

  private record RegisteredConnection(ClientConnection connection, ChatSession session) {}
}
