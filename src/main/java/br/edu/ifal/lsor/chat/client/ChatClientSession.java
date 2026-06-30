package br.edu.ifal.lsor.chat.client;

import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ChatClientSession implements AutoCloseable {

  private final ChatClientGateway gateway;

  public ChatClientSession(String host, int port, ChatClientListener listener) {
    this.gateway = new ChatClientGateway(host, port, event -> dispatch(event, listener));
  }

  public void connect() {
    gateway.connect();
  }

  public CompletableFuture<ServerResponse> login(String username, String displayName) {
    return gateway.login(username, displayName);
  }

  public CompletableFuture<ServerResponse> logout() {
    return gateway.logout();
  }

  public CompletableFuture<List<ClientUser>> listUsers() {
    return gateway.listUsers();
  }

  public CompletableFuture<List<ClientGroup>> listGroups() {
    return gateway.listGroups();
  }

  public CompletableFuture<List<ChatMessage>> history(ConversationTarget target) {
    return gateway.history(target);
  }

  public CompletableFuture<ServerResponse> sendMessage(ConversationTarget target, String text) {
    return gateway.sendMessage(target, text);
  }

  public CompletableFuture<ServerResponse> createGroup(String groupCode, String displayName) {
    return gateway.createGroup(groupCode, displayName);
  }

  public CompletableFuture<ServerResponse> joinGroup(String groupCode) {
    return gateway.joinGroup(groupCode);
  }

  public CompletableFuture<ServerResponse> leaveGroup(String groupCode) {
    return gateway.leaveGroup(groupCode);
  }

  public CompletableFuture<ServerResponse> renameGroup(String groupCode, String displayName) {
    return gateway.renameGroup(groupCode, displayName);
  }

  public CompletableFuture<ServerResponse> deleteGroup(String groupCode) {
    return gateway.deleteGroup(groupCode);
  }

  public String username() {
    return gateway.username();
  }

  @Override
  public void close() throws Exception {
    gateway.close();
  }

  private static void dispatch(ClientEvent event, ChatClientListener listener) {
    if (event instanceof ClientEvent.DirectMessage direct) {
      listener.onDirectMessage(direct.message());
    } else if (event instanceof ClientEvent.GroupMessage group) {
      listener.onGroupMessage(group.message());
    } else if (event instanceof ClientEvent.UserOnline online) {
      listener.onUserOnline(online.user());
    } else if (event instanceof ClientEvent.UserOffline offline) {
      listener.onUserOffline(offline.username());
    } else if (event instanceof ClientEvent.GroupsChanged) {
      listener.onGroupsChanged();
    } else if (event instanceof ClientEvent.Disconnected) {
      listener.onDisconnected();
    }
  }
}
