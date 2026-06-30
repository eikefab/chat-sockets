package br.edu.ifal.lsor.chat.client;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import br.edu.ifal.lsor.chat.socket.client.ChatClientSocket;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class ChatClientSession implements AutoCloseable {

  private final String host;
  private final int port;
  private final ChatClientListener listener;
  private ChatClientSocket socket;
  private String username;
  private volatile boolean closingGracefully;

  public ChatClientSession(String host, int port, ChatClientListener listener) {
    this.host = host;
    this.port = port;
    this.listener = listener;
  }

  public void connect() {
    socket = new ChatClientSocket(host, port, this::handleEvent, this::handleDisconnect);
    socket.openSocket();
  }

  public CompletableFuture<ServerResponse> login(String username, String displayName) {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("username", username);
    payload.put("displayName", displayName);
    return send(Actions.LOGIN, payload)
        .thenApply(
            response -> {
              if (response.isOk()) {
                this.username = username;
              }
              return response;
            });
  }

  public CompletableFuture<ServerResponse> logout() {
    closingGracefully = true;
    return send(Actions.LOGOUT, Map.of());
  }

  public CompletableFuture<List<ClientUser>> listUsers() {
    return send(Actions.LIST_USERS, Map.of())
        .thenApply(
            response -> {
              if (!response.isOk()) {
                throw new ChatResponseException(response);
              }
              return ChatPayloads.listFromPayload(response.payload(), "users").stream()
                  .map(ClientUser::fromPayload)
                  .toList();
            });
  }

  public CompletableFuture<List<ClientGroup>> listGroups() {
    return send(Actions.LIST_GROUPS, Map.of())
        .thenApply(
            response -> {
              if (!response.isOk()) {
                throw new ChatResponseException(response);
              }
              return ChatPayloads.listFromPayload(response.payload(), "groups").stream()
                  .map(ClientGroup::fromPayload)
                  .toList();
            });
  }

  public CompletableFuture<List<ChatMessage>> history(ConversationTarget target) {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("scope", target.kind().protocolValue());
    payload.put("target", target.id());
    return send(Actions.GET_HISTORY, payload)
        .thenApply(
            response -> {
              if (!response.isOk()) {
                throw new ChatResponseException(response);
              }
              return ChatPayloads.listFromPayload(response.payload(), "messages").stream()
                  .map(
                      message ->
                          ChatMessage.fromHistory(target.kind(), target.id(), username, message))
                  .toList();
            });
  }

  public CompletableFuture<ServerResponse> sendMessage(ConversationTarget target, String text) {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("text", text);
    if (target.kind() == ConversationKind.GROUP) {
      payload.put("groupCode", target.id());
      return send(Actions.SEND_GROUP, payload);
    }
    payload.put("targetUsername", target.id());
    return send(Actions.SEND_DIRECT, payload);
  }

  public CompletableFuture<ServerResponse> createGroup(String groupCode, String displayName) {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("groupCode", stripGroupPrefix(groupCode));
    payload.put("displayName", displayName);
    return send(Actions.CREATE_GROUP, payload);
  }

  public CompletableFuture<ServerResponse> joinGroup(String groupCode) {
    return send(Actions.JOIN_GROUP, Map.of("groupCode", stripGroupPrefix(groupCode)));
  }

  public CompletableFuture<ServerResponse> leaveGroup(String groupCode) {
    return send(Actions.LEAVE_GROUP, Map.of("groupCode", stripGroupPrefix(groupCode)));
  }

  public CompletableFuture<ServerResponse> renameGroup(String groupCode, String displayName) {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("groupCode", stripGroupPrefix(groupCode));
    payload.put("displayName", displayName);
    return send(Actions.RENAME_GROUP, payload);
  }

  public CompletableFuture<ServerResponse> deleteGroup(String groupCode) {
    return send(Actions.DELETE_GROUP, Map.of("groupCode", stripGroupPrefix(groupCode)));
  }

  public String username() {
    return username;
  }

  private CompletableFuture<ServerResponse> send(String action, Map<String, Serializable> payload) {
    if (socket == null) {
      CompletableFuture<ServerResponse> failed = new CompletableFuture<>();
      failed.completeExceptionally(new IllegalStateException("Cliente não conectado."));
      return failed;
    }
    return socket.send(action, payload);
  }

  private void handleEvent(ServerEvent event) {
    Map<String, Serializable> payload = event.payload();
    switch (event.eventType()) {
      case Events.DIRECT_MESSAGE -> {
        String from = (String) payload.get("fromUsername");
        String text = (String) payload.get("text");
        Instant createdAt = (Instant) payload.get("createdAt");
        listener.onDirectMessage(
            new ChatMessage(ConversationKind.DIRECT, from, from, text, createdAt, false));
      }
      case Events.GROUP_MESSAGE -> {
        String groupCode = (String) payload.get("groupCode");
        String author = (String) payload.get("authorUsername");
        String text = (String) payload.get("text");
        Instant createdAt = (Instant) payload.get("createdAt");
        listener.onGroupMessage(
            new ChatMessage(ConversationKind.GROUP, groupCode, author, text, createdAt, false));
      }
      case Events.USER_ONLINE -> listener.onUserOnline(ClientUser.fromPayload(payload));
      case Events.USER_OFFLINE -> listener.onUserOffline((String) payload.get("username"));
      case Events.GROUP_CREATED -> {
        listener.onGroupsChanged();
        listener.onSystemMessage("Grupo criado: #" + payload.get("groupCode"));
      }
      case Events.GROUP_RENAMED -> {
        listener.onGroupsChanged();
        listener.onSystemMessage("Grupo renomeado: #" + payload.get("groupCode"));
      }
      case Events.GROUP_DELETED -> {
        listener.onGroupsChanged();
        listener.onSystemMessage("Grupo excluído: #" + payload.get("groupCode"));
      }
      case Events.GROUP_MEMBER_JOINED -> listener.onGroupsChanged();
      case Events.GROUP_MEMBER_LEFT -> listener.onGroupsChanged();
      default -> {}
    }
  }

  private void handleDisconnect() {
    if (!closingGracefully) {
      listener.onDisconnected();
    }
  }

  private static String stripGroupPrefix(String groupCode) {
    String trimmed = groupCode == null ? "" : groupCode.trim();
    return trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
  }

  @Override
  public void close() throws Exception {
    closingGracefully = true;
    if (socket != null) {
      try {
        if (username != null) {
          logout().get(3, TimeUnit.SECONDS);
        }
      } catch (Exception ignored) {
      }
      socket.close();
    }
  }

  public static final class ChatResponseException extends RuntimeException {

    private final ServerResponse response;

    ChatResponseException(ServerResponse response) {
      super(response.message());
      this.response = response;
    }

    public ServerResponse response() {
      return response;
    }
  }
}
