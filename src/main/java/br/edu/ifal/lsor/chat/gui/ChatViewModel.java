package br.edu.ifal.lsor.chat.gui;

import br.edu.ifal.lsor.chat.client.ChatClientGateway;
import br.edu.ifal.lsor.chat.client.ChatMessage;
import br.edu.ifal.lsor.chat.client.ChatPayloads;
import br.edu.ifal.lsor.chat.client.ClientEvent;
import br.edu.ifal.lsor.chat.client.ClientGroup;
import br.edu.ifal.lsor.chat.client.ClientUser;
import br.edu.ifal.lsor.chat.client.ConversationKind;
import br.edu.ifal.lsor.chat.client.ConversationTarget;
import br.edu.ifal.lsor.chat.client.GroupEventKind;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

final class ChatViewModel implements AutoCloseable {

  private final ChatClientGateway client;
  private final ObservableList<ConversationTarget> conversations =
      FXCollections.observableArrayList();
  private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
  private final ObjectProperty<ConversationTarget> selectedConversation =
      new SimpleObjectProperty<>();
  private final StringProperty title = new SimpleStringProperty("Selecione uma conversa");
  private final StringProperty status = new SimpleStringProperty();
  private final BooleanProperty sending = new SimpleBooleanProperty(false);
  private final ObjectProperty<SelectedGroupDetails> selectedGroupDetails =
      new SimpleObjectProperty<>(SelectedGroupDetails.none());
  private final BooleanBinding selectedGroup =
      Bindings.createBooleanBinding(
          () -> selectedGroupDetails.get().present(), selectedGroupDetails);
  private final BooleanBinding selectedGroupOwned =
      Bindings.createBooleanBinding(() -> selectedGroupDetails.get().owned(), selectedGroupDetails);
  private final BooleanBinding selectedGroupMember =
      Bindings.createBooleanBinding(
          () -> selectedGroupDetails.get().member(), selectedGroupDetails);
  private final BooleanBinding canSendMessage =
      Bindings.createBooleanBinding(
          this::hasWritableSelection, selectedConversation, selectedGroupDetails);
  private final StringBinding groupMembers =
      Bindings.createStringBinding(
          () -> selectedGroupDetails.get().memberSummary(), selectedGroupDetails);
  private final AtomicInteger directoryVersion = new AtomicInteger();
  private final List<ClientUser> users = new ArrayList<>();
  private final List<ClientGroup> groups = new ArrayList<>();

  ChatViewModel(String host, int port) {
    this.client = new ChatClientGateway(host, port, this::handleEvent);
    selectedConversation.addListener(
        (observable, oldValue, target) -> {
          updateSelectedGroupDetails();
          if (!Objects.equals(target, oldValue)) {
            loadHistory(target);
          }
        });
  }

  ObservableList<ConversationTarget> conversations() {
    return conversations;
  }

  ObservableList<ChatMessage> messages() {
    return messages;
  }

  ObjectProperty<ConversationTarget> selectedConversationProperty() {
    return selectedConversation;
  }

  StringProperty titleProperty() {
    return title;
  }

  StringProperty statusProperty() {
    return status;
  }

  BooleanProperty sendingProperty() {
    return sending;
  }

  BooleanBinding selectedGroupOwnedProperty() {
    return selectedGroupOwned;
  }

  BooleanBinding selectedGroupMemberProperty() {
    return selectedGroupMember;
  }

  BooleanBinding hasSelectedGroupProperty() {
    return selectedGroup;
  }

  BooleanBinding canSendMessageProperty() {
    return canSendMessage;
  }

  StringBinding groupMembersProperty() {
    return groupMembers;
  }

  boolean isUserOnline(String username) {
    return users.stream().anyMatch(user -> user.username().equals(username) && user.online());
  }

  void selectConversation(ConversationTarget target) {
    selectedConversation.set(target);
  }

  String username() {
    return client.username();
  }

  void connectAndLogin(
      String username,
      String displayName,
      Consumer<ServerResponse> onSuccess,
      Consumer<String> onFailure) {
    status.set("Conectando...");
    CompletableFuture.runAsync(
            () -> {
              client.connect();
            })
        .thenCompose(ignored -> client.login(username, displayName))
        .whenComplete(
            (response, error) ->
                FxDispatch.run(
                    () -> {
                      if (error != null) {
                        closeQuietly();
                        onFailure.accept("Falha ao conectar: " + FxDispatch.rootMessage(error));
                        return;
                      }
                      if (!response.isOk()) {
                        closeQuietly();
                        onFailure.accept("Falha no login: " + response.message());
                        return;
                      }
                      status.set("Conectado como " + username);
                      onSuccess.accept(response);
                      refreshDirectory();
                    }));
  }

  void refreshDirectory() {
    int version = directoryVersion.incrementAndGet();
    status.set("Atualizando contatos e grupos...");
    client
        .listUsers()
        .thenCombine(client.listGroups(), DirectorySnapshot::new)
        .whenComplete(
            (snapshot, error) ->
                FxDispatch.run(
                    () -> {
                      if (version != directoryVersion.get()) {
                        return;
                      }
                      if (error != null) {
                        status.set("Erro ao atualizar: " + FxDispatch.rootMessage(error));
                        return;
                      }
                      applyDirectory(snapshot);
                      status.set("Pronto.");
                    }));
  }

  void sendSelectedMessage(String rawText) {
    ConversationTarget target = selectedConversation.get();
    String text = rawText.trim();
    if (target == null || text.isEmpty()) {
      return;
    }
    if (!canSendTo(target)) {
      status.set("Entre no grupo para enviar mensagens.");
      return;
    }
    sending.set(true);
    client
        .sendMessage(target, text)
        .whenComplete(
            (response, error) ->
                FxDispatch.run(
                    () -> {
                      sending.set(false);
                      if (error != null) {
                        status.set("Erro ao enviar: " + FxDispatch.rootMessage(error));
                        return;
                      }
                      if (!response.isOk()) {
                        status.set("Erro: " + response.message());
                        return;
                      }
                      if (target.kind() == ConversationKind.DIRECT) {
                        java.time.Instant createdAt =
                            ChatPayloads.instant(response.payload(), "createdAt");
                        appendIfCurrent(
                            new ChatMessage(
                                target.kind(),
                                target.id(),
                                client.username(),
                                text,
                                createdAt,
                                true));
                      }
                    }));
  }

  void createGroup(String groupCode, String displayName) {
    runGroupAction(client.createGroup(groupCode, displayName));
  }

  void joinGroup(String groupCode) {
    runGroupAction(client.joinGroup(groupCode));
  }

  void leaveSelectedGroup() {
    selectedGroupCode().ifPresent(groupCode -> runGroupAction(client.leaveGroup(groupCode)));
  }

  void deleteSelectedGroup() {
    selectedGroupCode().ifPresent(groupCode -> runGroupAction(client.deleteGroup(groupCode)));
  }

  private void loadHistory(ConversationTarget target) {
    messages.clear();
    if (target == null) {
      title.set("Selecione uma conversa");
      return;
    }
    title.set(target.label());
    if (!canLoadHistory(target)) {
      status.set("Entre no grupo para ver o histórico.");
      return;
    }
    status.set("Carregando histórico...");
    client
        .history(target)
        .whenComplete(
            (history, error) ->
                FxDispatch.run(
                    () -> {
                      if (error != null) {
                        status.set("Erro ao carregar histórico: " + FxDispatch.rootMessage(error));
                        return;
                      }
                      messages.setAll(history);
                      status.set("Histórico carregado.");
                    }));
  }

  private void runGroupAction(CompletableFuture<ServerResponse> action) {
    action.whenComplete(
        (response, error) ->
            FxDispatch.run(
                () -> {
                  if (error != null) {
                    status.set("Erro: " + FxDispatch.rootMessage(error));
                    return;
                  }
                  status.set(response.message());
                  if (response.isOk()) {
                    refreshDirectory();
                  }
                }));
  }

  private java.util.Optional<String> selectedGroupCode() {
    ConversationTarget target = selectedConversation.get();
    if (target == null || target.kind() != ConversationKind.GROUP) {
      status.set("Selecione um grupo.");
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(target.id());
  }

  private void applyDirectory(DirectorySnapshot snapshot) {
    ConversationTarget selected = selectedConversation.get();
    users.clear();
    users.addAll(snapshot.users());
    groups.clear();
    groups.addAll(snapshot.groups());
    conversations.setAll(
        users.stream()
            .filter(user -> !user.username().equals(client.username()))
            .map(ConversationTarget::user)
            .toList());
    conversations.addAll(groups.stream().map(ConversationTarget::group).toList());
    reselect(selected);
    updateSelectedGroupDetails();
  }

  private void reselect(ConversationTarget selected) {
    if (selected == null) {
      return;
    }
    conversations.stream()
        .filter(target -> target.kind() == selected.kind() && target.id().equals(selected.id()))
        .findFirst()
        .ifPresent(
            target -> {
              if (!Objects.equals(target, selectedConversation.get())) {
                selectedConversation.set(target);
              }
            });
  }

  private void updateSelectedGroupDetails() {
    ConversationTarget target = selectedConversation.get();
    selectedGroupDetails.set(selectedGroupDetailsFor(target));
  }

  private SelectedGroupDetails selectedGroupDetailsFor(ConversationTarget target) {
    if (target == null || target.kind() != ConversationKind.GROUP) {
      return SelectedGroupDetails.none();
    }
    return groups.stream()
        .filter(group -> group.groupCode().equals(target.id()))
        .findFirst()
        .map(this::selectedGroupDetails)
        .orElseGet(SelectedGroupDetails::none);
  }

  private SelectedGroupDetails selectedGroupDetails(ClientGroup group) {
    return new SelectedGroupDetails(
        true,
        group.ownerUsername().equals(client.username()),
        group.member(),
        formatGroupMembers(group));
  }

  private boolean hasWritableSelection() {
    ConversationTarget target = selectedConversation.get();
    return target != null && canSendTo(target);
  }

  private boolean canSendTo(ConversationTarget target) {
    return target.kind() == ConversationKind.DIRECT || selectedGroupDetails.get().member();
  }

  private boolean canLoadHistory(ConversationTarget target) {
    return target.kind() == ConversationKind.DIRECT || selectedGroupDetails.get().member();
  }

  private String formatGroupMembers(ClientGroup group) {
    List<String> memberUsernames = group.memberUsernames();
    long onlineCount = memberUsernames.stream().filter(this::isUserOnline).count();
    String names =
        memberUsernames.stream().map(this::displayNameForMember).collect(Collectors.joining(", "));
    return memberUsernames.size()
        + " membro"
        + (memberUsernames.size() == 1 ? "" : "s")
        + " ("
        + onlineCount
        + " online)"
        + (names.isEmpty() ? "" : " - " + names);
  }

  private String displayNameForMember(String username) {
    if (username.equals(client.username())) {
      return "Você";
    }
    return users.stream()
        .filter(user -> user.username().equals(username))
        .findFirst()
        .map(ClientUser::displayName)
        .orElse(username);
  }

  private void handleEvent(ClientEvent event) {
    FxDispatch.run(
        () -> {
          if (event instanceof ClientEvent.DirectMessage direct) {
            appendIfCurrent(direct.message());
          } else if (event instanceof ClientEvent.GroupMessage group) {
            appendIfCurrent(group.message());
          } else if (event instanceof ClientEvent.UserOnline
              || event instanceof ClientEvent.UserOffline) {
            refreshDirectory();
          } else if (event instanceof ClientEvent.GroupsChanged group) {
            status.set(groupStatus(group.kind(), group.groupCode()));
            refreshDirectory();
          } else if (event instanceof ClientEvent.Disconnected) {
            status.set("Conexão com o servidor perdida.");
            sending.set(false);
          }
        });
  }

  private void appendIfCurrent(ChatMessage message) {
    ConversationTarget selected = selectedConversation.get();
    if (selected != null
        && selected.kind() == message.kind()
        && selected.id().equals(message.conversationId())) {
      messages.add(message);
    }
  }

  private static String groupStatus(GroupEventKind kind, String groupCode) {
    String label = groupCode == null || groupCode.isEmpty() ? "grupo" : "#" + groupCode;
    return switch (kind) {
      case CREATED -> "Grupo criado: " + label;
      case RENAMED -> "Grupo renomeado: " + label;
      case DELETED -> "Grupo excluído: " + label;
      case MEMBER_JOINED -> "Membro entrou em " + label;
      case MEMBER_LEFT -> "Membro saiu de " + label;
    };
  }

  private void closeQuietly() {
    try {
      client.close();
    } catch (Exception ignored) {
    }
  }

  @Override
  public void close() throws Exception {
    client.close();
  }

  private record DirectorySnapshot(List<ClientUser> users, List<ClientGroup> groups) {}

  private record SelectedGroupDetails(
      boolean present, boolean owned, boolean member, String memberSummary) {

    static SelectedGroupDetails none() {
      return new SelectedGroupDetails(false, false, false, "");
    }
  }
}
