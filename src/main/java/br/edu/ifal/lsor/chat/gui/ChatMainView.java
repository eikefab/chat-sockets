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
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ChatMainView {

  private final ChatViewModel model;
  private final ListView<ConversationTarget> conversationList;
  private final TextArea messageInput = new TextArea();
  private final BorderPane root;

  ChatMainView(ChatViewModel model) {
    this.model = model;
    this.conversationList = new ListView<>(model.conversations());
    conversationList.setCellFactory(view -> new ConversationCell());
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
    Button refreshButton = new Button("Atualizar");
    refreshButton.setOnAction(event -> model.refreshDirectory());
    Button createButton = new Button("Criar");
    createButton.setOnAction(
        event ->
            GroupDialogs.createGroup()
                .ifPresent(input -> model.createGroup(input.groupCode(), input.displayName())));
    Button joinButton = new Button("Entrar");
    joinButton.setOnAction(event -> GroupDialogs.askGroupCode().ifPresent(model::joinGroup));
    Button renameButton = new Button("Renomear");
    renameButton.setOnAction(
        event -> GroupDialogs.askNewName().ifPresent(model::renameSelectedGroup));
    Button leaveButton = new Button("Sair");
    leaveButton.setOnAction(event -> model.leaveSelectedGroup());
    Button deleteButton = new Button("Excluir");
    deleteButton.setOnAction(
        event -> {
          ConversationTarget selected = model.selectedConversationProperty().get();
          if (selected != null && GroupDialogs.confirmDelete(selected.label())) {
            model.deleteSelectedGroup();
          }
        });

    VBox sidebar =
        new VBox(
            10,
            new Label("Conversas"),
            conversationList,
            new Separator(),
            new HBox(8, refreshButton, createButton, joinButton),
            new HBox(8, renameButton, leaveButton, deleteButton));
    sidebar.setPadding(new Insets(16));
    sidebar.setPrefWidth(310);
    VBox.setVgrow(conversationList, Priority.ALWAYS);
    return sidebar;
  }

  private VBox center(ListView<ChatMessage> messageList) {
    Label titleLabel = new Label();
    titleLabel.textProperty().bind(model.titleProperty());
    titleLabel.getStyleClass().add("title");
    Label statusLabel = new Label();
    statusLabel.textProperty().bind(model.statusProperty());
    statusLabel.getStyleClass().add("status");

    messageInput.setPromptText("Escreva uma mensagem");
    messageInput.setWrapText(true);
    messageInput.setPrefRowCount(3);
    messageInput.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER && event.isShortcutDown()) {
            sendMessage();
          }
        });

    Button sendButton = new Button("Enviar");
    sendButton.setDefaultButton(true);
    sendButton
        .disableProperty()
        .bind(
            Bindings.createBooleanBinding(
                () ->
                    model.selectedConversationProperty().get() == null
                        || model.sendingProperty().get(),
                model.selectedConversationProperty(),
                model.sendingProperty()));
    sendButton.setOnAction(event -> sendMessage());

    HBox composer = new HBox(10, messageInput, sendButton);
    composer.setAlignment(Pos.CENTER);
    composer.setPadding(new Insets(12, 0, 0, 0));
    HBox.setHgrow(messageInput, Priority.ALWAYS);

    VBox center = new VBox(10, titleLabel, messageList, composer, statusLabel);
    center.setPadding(new Insets(16));
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

  private static void scroll(ListView<?> listView) {
    if (!listView.getItems().isEmpty()) {
      listView.scrollTo(listView.getItems().size() - 1);
    }
  }
}
