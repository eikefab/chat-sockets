package br.edu.ifal.lsor.chat.gui;

import br.edu.ifal.lsor.chat.client.ChatMessage;
import br.edu.ifal.lsor.chat.client.ConversationTarget;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

final class ChatMainView {

  private final ChatViewModel model;
  private final ListView<ConversationTarget> conversationList;
  private final TextField messageInput = new TextField();
  private final BorderPane root;
  private final Runnable onQuit;

  ChatMainView(ChatViewModel model, Runnable onQuit) {
    this.model = model;
    this.onQuit = onQuit;
    this.conversationList = new ListView<>(model.conversations());
    conversationList.setCellFactory(view -> new ConversationCell(model::isUserOnline));
    conversationList
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((observable, oldValue, target) -> model.selectConversation(target));
    model
        .selectedConversationProperty()
        .addListener(
            (observable, oldValue, target) -> {
              if (target != conversationList.getSelectionModel().getSelectedItem()) {
                conversationList.getSelectionModel().select(target);
              }
            });

    ListView<ChatMessage> messageList = new ListView<>(model.messages());
    messageList.setCellFactory(view -> new MessageCell());
    messageList.getStyleClass().add("message-list");
    model
        .messages()
        .addListener(
            (javafx.collections.ListChangeListener.Change<?> change) -> scroll(messageList));

    root = new BorderPane(center(messageList));
    root.setLeft(sidebar());
    root.getStyleClass().add("root");
  }

  Parent root() {
    return root;
  }

  private VBox sidebar() {
    MenuButton groupsMenu = new MenuButton("Grupos", Icons.accountGroup());
    groupsMenu.getStyleClass().add("groups-menu");
    groupsMenu.setMaxWidth(Double.MAX_VALUE);
    MenuItem createItem = new MenuItem("Criar", Icons.plus());
    createItem.setOnAction(
        event ->
            GroupDialogs.createGroup()
                .ifPresent(input -> model.createGroup(input.groupCode(), input.displayName())));
    MenuItem joinItem = new MenuItem("Entrar", Icons.importIcon());
    joinItem.setOnAction(event -> GroupDialogs.askGroupCode().ifPresent(model::joinGroup));
    groupsMenu.getItems().addAll(createItem, joinItem);

    Label eyebrow = new Label("CONVERSAS");
    eyebrow.getStyleClass().add("eyebrow");
    Region divider = new Region();
    divider.getStyleClass().add("sidebar-divider");

    Button quitButton = new Button("Sair", Icons.logout());
    quitButton.getStyleClass().add("danger");
    quitButton.setMaxWidth(Double.MAX_VALUE);
    quitButton.setTooltip(new Tooltip("Sair do chat"));
    quitButton.setOnAction(event -> onQuit.run());

    VBox sidebar = new VBox(12, groupsMenu, eyebrow, conversationList, divider, quitButton);
    sidebar.setPadding(new Insets(16));
    sidebar.setPrefWidth(310);
    sidebar.setSpacing(12);
    sidebar.getStyleClass().add("sidebar");
    VBox.setVgrow(conversationList, Priority.ALWAYS);
    return sidebar;
  }

  private VBox center(ListView<ChatMessage> messageList) {
    Label titleLabel = new Label();
    titleLabel.textProperty().bind(model.titleProperty());
    titleLabel.getStyleClass().add("title");

    Label membersLabel = new Label();
    membersLabel.textProperty().bind(model.groupMembersProperty());
    membersLabel.getStyleClass().add("group-members");
    membersLabel.visibleProperty().bind(model.hasSelectedGroupProperty());
    membersLabel.managedProperty().bind(membersLabel.visibleProperty());

    Label statusLabel = new Label();
    statusLabel.textProperty().bind(model.statusProperty());
    statusLabel.getStyleClass().add("status-pill");

    Button leaveGroupButton = new Button("Sair", Icons.logout());
    leaveGroupButton.getStyleClass().add("danger");
    leaveGroupButton.setTooltip(new Tooltip("Sair do grupo selecionado"));
    leaveGroupButton.setOnAction(event -> model.leaveSelectedGroup());

    Button deleteGroupButton = new Button("Excluir", Icons.trash());
    deleteGroupButton.getStyleClass().add("danger");
    deleteGroupButton.setTooltip(new Tooltip("Excluir grupo selecionado"));
    deleteGroupButton.setOnAction(
        event -> {
          ConversationTarget selected = model.selectedConversationProperty().get();
          if (selected != null && GroupDialogs.confirmDelete(selected.label())) {
            model.deleteSelectedGroup();
          }
        });

    leaveGroupButton
        .visibleProperty()
        .bind(model.selectedGroupMemberProperty().and(model.selectedGroupOwnedProperty().not()));
    leaveGroupButton.managedProperty().bind(leaveGroupButton.visibleProperty());
    deleteGroupButton
        .visibleProperty()
        .bind(model.hasSelectedGroupProperty().and(model.selectedGroupOwnedProperty()));
    deleteGroupButton.managedProperty().bind(deleteGroupButton.visibleProperty());

    VBox titleBox = new VBox(2, titleLabel, membersLabel);
    HBox header =
        new HBox(12, titleBox, statusLabel, spacer(), leaveGroupButton, deleteGroupButton);
    header.setAlignment(Pos.CENTER_LEFT);

    messageInput.setPromptText("Escreva uma mensagem");
    messageInput.getStyleClass().add("message-input");
    messageInput.setPrefHeight(40);
    messageInput.setOnAction(event -> sendMessage());

    Button sendButton = new Button("Enviar");
    sendButton.getStyleClass().add("primary");
    sendButton.setGraphic(Icons.send());
    sendButton.setPrefHeight(40);
    sendButton
        .disableProperty()
        .bind(
            Bindings.createBooleanBinding(
                () -> !model.canSendMessageProperty().get() || model.sendingProperty().get(),
                model.canSendMessageProperty(),
                model.sendingProperty()));
    sendButton.setOnAction(event -> sendMessage());

    HBox composer = new HBox(10, messageInput, sendButton);
    composer.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(messageInput, Priority.ALWAYS);

    VBox center = new VBox(10, header, messageList, composer);
    center.setPadding(new Insets(16));
    center.getStyleClass().add("chat-center");
    VBox.setVgrow(messageList, Priority.ALWAYS);
    return center;
  }

  private void sendMessage() {
    String text = messageInput.getText();
    if (!text.trim().isEmpty()) {
      messageInput.clear();
      model.sendSelectedMessage(text);
    }
  }

  private static Region spacer() {
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    return spacer;
  }

  private static void scroll(ListView<?> listView) {
    if (!listView.getItems().isEmpty()) {
      listView.scrollTo(listView.getItems().size() - 1);
    }
  }
}
