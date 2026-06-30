package br.edu.ifal.lsor.chat.client;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import br.edu.ifal.lsor.chat.socket.client.ChatClientSocket;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ChatClientGateway implements AutoCloseable {

  private final String host;
  private final int port;
  private final Consumer<ClientEvent> eventListener;
  private final ClientEventMapper eventMapper = new ClientEventMapper();
  private ChatClientSocket socket;
  private String username;
  private volatile boolean closingGracefully;

  public ChatClientGateway(String host, int port, Consumer<ClientEvent> eventListener) {
    this.host = host;
    this.port = port;
    this.eventListener = eventListener;
  }

  public void connect() {
    socket = new ChatClientSocket(host, port, this::handleEvent, this::handleDisconnect);
    socket.openSocket();
  }

  public CompletableFuture<ServerResponse> login(String username, String displayName) {
    return send(Actions.LOGIN, payload("username", username, "displayName", displayName))
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
    return send(Actions.LIST_USERS, Map.of()).thenApply(ChatClientGateway::usersFromResponse);
  }

  public CompletableFuture<List<ClientGroup>> listGroups() {
    return send(Actions.LIST_GROUPS, Map.of()).thenApply(ChatClientGateway::groupsFromResponse);
  }

  public CompletableFuture<List<ChatMessage>> history(ConversationTarget target) {
    return send(
            Actions.GET_HISTORY,
            payload("scope", target.kind().protocolValue(), "target", target.id()))
        .thenApply(response -> messagesFromResponse(response, target, username));
  }

  public CompletableFuture<ServerResponse> sendMessage(ConversationTarget target, String text) {
    if (target.kind() == ConversationKind.GROUP) {
      return send(Actions.SEND_GROUP, payload("groupCode", target.id(), "text", text));
    }
    return send(Actions.SEND_DIRECT, payload("targetUsername", target.id(), "text", text));
  }

  public CompletableFuture<ServerResponse> createGroup(String groupCode, String displayName) {
    return send(
        Actions.CREATE_GROUP,
        payload("groupCode", stripGroupPrefix(groupCode), "displayName", displayName));
  }

  public CompletableFuture<ServerResponse> joinGroup(String groupCode) {
    return send(Actions.JOIN_GROUP, payload("groupCode", stripGroupPrefix(groupCode)));
  }

  public CompletableFuture<ServerResponse> leaveGroup(String groupCode) {
    return send(Actions.LEAVE_GROUP, payload("groupCode", stripGroupPrefix(groupCode)));
  }

  public CompletableFuture<ServerResponse> renameGroup(String groupCode, String displayName) {
    return send(
        Actions.RENAME_GROUP,
        payload("groupCode", stripGroupPrefix(groupCode), "displayName", displayName));
  }

  public CompletableFuture<ServerResponse> deleteGroup(String groupCode) {
    return send(Actions.DELETE_GROUP, payload("groupCode", stripGroupPrefix(groupCode)));
  }

  public String username() {
    return username;
  }

  private CompletableFuture<ServerResponse> send(
      String action, Map<String, Serializable> requestPayload) {
    if (socket == null) {
      CompletableFuture<ServerResponse> failed = new CompletableFuture<>();
      failed.completeExceptionally(new IllegalStateException("Cliente não conectado."));
      return failed;
    }
    return socket.send(action, requestPayload);
  }

  private void handleEvent(br.edu.ifal.lsor.chat.protocol.ServerEvent event) {
    eventMapper.map(event, username).ifPresent(eventListener);
  }

  private void handleDisconnect() {
    if (!closingGracefully) {
      eventListener.accept(new ClientEvent.Disconnected());
    }
  }

  private static List<ClientUser> usersFromResponse(ServerResponse response) {
    ensureOk(response);
    return ChatPayloads.listFromPayload(response.payload(), "users").stream()
        .map(ClientUser::fromPayload)
        .toList();
  }

  private static List<ClientGroup> groupsFromResponse(ServerResponse response) {
    ensureOk(response);
    return ChatPayloads.listFromPayload(response.payload(), "groups").stream()
        .map(ClientGroup::fromPayload)
        .toList();
  }

  private static List<ChatMessage> messagesFromResponse(
      ServerResponse response, ConversationTarget target, String username) {
    ensureOk(response);
    return ChatPayloads.listFromPayload(response.payload(), "messages").stream()
        .map(message -> ChatMessage.fromHistory(target.kind(), target.id(), username, message))
        .toList();
  }

  private static void ensureOk(ServerResponse response) {
    if (!response.isOk()) {
      throw new ChatResponseException(response);
    }
  }

  private static Map<String, Serializable> payload(String key, Serializable value) {
    return Map.of(key, value);
  }

  private static Map<String, Serializable> payload(
      String firstKey, Serializable firstValue, String secondKey, Serializable secondValue) {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put(firstKey, firstValue);
    payload.put(secondKey, secondValue);
    return Map.copyOf(payload);
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
}
