package br.edu.ifal.lsor.chat.gui;

import br.edu.ifal.lsor.chat.client.ChatClientListener;
import br.edu.ifal.lsor.chat.client.ChatClientSession;
import br.edu.ifal.lsor.chat.client.ChatMessage;
import br.edu.ifal.lsor.chat.client.ClientGroup;
import br.edu.ifal.lsor.chat.client.ClientUser;
import br.edu.ifal.lsor.chat.client.ConversationKind;
import br.edu.ifal.lsor.chat.client.ConversationTarget;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class ChatFxApplication extends Application {

  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
  private static ClientConfig initialConfig;

  private final ObservableList<ConversationTarget> conversations =
      FXCollections.observableArrayList();
  private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
  private final List<ClientUser> users = new ArrayList<>();
  private final List<ClientGroup> groups = new ArrayList<>();

  private Stage stage;
  private ChatClientSession session;
  private ListView<ConversationTarget> conversationList;
  private ListView<ChatMessage> messageList;
  private Label statusLabel;
  private Label titleLabel;
  private TextArea messageInput;
  private Button sendButton;

  public static void launchClient(String host, int port) {
    initialConfig = new ClientConfig(host, port);
    launch();
  }

  @Override
  public void start(Stage primaryStage) {
    this.stage = primaryStage;
    stage.setTitle("Chat IFAL");
    stage.setMinWidth(920);
    stage.setMinHeight(620);
    showLogin(initialConfig.host(), initialConfig.port());
    stage.show();
  }

  @Override
  public void stop() throws Exception {
    if (session != null) {
      session.close();
    }
  }

  private void showLogin(String host, int port) {
    TextField hostField = new TextField(host);
    TextField portField = new TextField(String.valueOf(port));
    TextField usernameField = new TextField();
    TextField displayNameField = new TextField();

    hostField.setPromptText("Host");
    portField.setPromptText("Porta");
    usernameField.setPromptText("Nome de usuário");
    displayNameField.setPromptText("Nome público");

    Button connectButton = new Button("Entrar");
    statusLabel = new Label();
    statusLabel.getStyleClass().add("status");

    HBox serverFields = new HBox(8, hostField, portField);
    HBox.setHgrow(hostField, Priority.ALWAYS);
    portField.setMaxWidth(100);

    VBox form =
        new VBox(
            12,
            new Label("Chat IFAL"),
            serverFields,
            usernameField,
            displayNameField,
            connectButton,
            statusLabel);
    form.setAlignment(Pos.CENTER);
    form.setPadding(new Insets(32));
    form.setMaxWidth(360);
    form.getStyleClass().add("login");

    BorderPane root = new BorderPane(form);
    root.getStyleClass().add("root");
    Scene scene = new Scene(root, 920, 620);
    scene.getStylesheets().add(stylesheet());
    stage.setScene(scene);

    Runnable submit =
        () ->
            connectAndLogin(
                hostField.getText(),
                portField.getText(),
                usernameField.getText(),
                displayNameField.getText(),
                connectButton);
    connectButton.setOnAction(event -> submit.run());
    displayNameField.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER) {
            submit.run();
          }
        });
  }

  private void connectAndLogin(
      String host, String portText, String username, String displayName, Button connectButton) {
    String cleanUser = username.trim();
    String cleanDisplay = displayName.trim().isEmpty() ? cleanUser : displayName.trim();
    if (cleanUser.isEmpty()) {
      setStatus("Informe o nome de usuário.");
      return;
    }

    int port;
    try {
      port = Integer.parseInt(portText.trim());
    } catch (NumberFormatException exception) {
      setStatus("Porta inválida.");
      return;
    }

    connectButton.setDisable(true);
    setStatus("Conectando...");
    CompletableFuture.runAsync(
            () -> {
              session = new ChatClientSession(host.trim(), port, new FxClientListener());
              session.connect();
            })
        .thenCompose(ignored -> session.login(cleanUser, cleanDisplay))
        .whenComplete(
            (response, error) ->
                Platform.runLater(
                    () -> {
                      connectButton.setDisable(false);
                      if (error != null) {
                        setStatus("Falha ao conectar: " + rootMessage(error));
                        closeQuietly();
                        return;
                      }
                      if (!response.isOk()) {
                        setStatus("Falha no login: " + response.message());
                        closeQuietly();
                        return;
                      }
                      showChat(cleanUser);
                      refreshDirectory();
                    }));
  }

  private void showChat(String username) {
    conversationList = new ListView<>(conversations);
    conversationList.setCellFactory(view -> new ConversationCell());
    conversationList
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((observable, oldValue, target) -> loadHistory(target));

    messageList = new ListView<>(messages);
    messageList.setCellFactory(view -> new MessageCell());

    titleLabel = new Label("Selecione uma conversa");
    titleLabel.getStyleClass().add("title");
    statusLabel = new Label("Conectado como " + username);
    statusLabel.getStyleClass().add("status");

    Button refreshButton = new Button("Atualizar");
    refreshButton.setOnAction(event -> refreshDirectory());

    Button createButton = new Button("Criar");
    createButton.setOnAction(event -> createGroup());

    Button joinButton = new Button("Entrar");
    joinButton.setOnAction(event -> joinGroup());

    Button renameButton = new Button("Renomear");
    renameButton.setOnAction(event -> renameSelectedGroup());

    Button leaveButton = new Button("Sair");
    leaveButton.setOnAction(event -> leaveSelectedGroup());

    Button deleteButton = new Button("Excluir");
    deleteButton.setOnAction(event -> deleteSelectedGroup());

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

    messageInput = new TextArea();
    messageInput.setPromptText("Escreva uma mensagem");
    messageInput.setWrapText(true);
    messageInput.setPrefRowCount(3);
    messageInput.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER && event.isShortcutDown()) {
            sendSelectedMessage();
          }
        });

    sendButton = new Button("Enviar");
    sendButton.setDefaultButton(true);
    sendButton.setOnAction(event -> sendSelectedMessage());
    sendButton.setDisable(true);

    HBox composer = new HBox(10, messageInput, sendButton);
    composer.setAlignment(Pos.CENTER);
    composer.setPadding(new Insets(12, 0, 0, 0));
    HBox.setHgrow(messageInput, Priority.ALWAYS);

    VBox center = new VBox(10, titleLabel, messageList, composer, statusLabel);
    center.setPadding(new Insets(16));
    VBox.setVgrow(messageList, Priority.ALWAYS);

    BorderPane root = new BorderPane(center);
    root.setLeft(sidebar);
    root.getStyleClass().add("root");

    Scene scene = new Scene(root, 1040, 680);
    scene.getStylesheets().add(stylesheet());
    stage.setScene(scene);
  }

  private void refreshDirectory() {
    if (session == null) {
      return;
    }
    setStatus("Atualizando contatos e grupos...");
    session
        .listUsers()
        .thenCombine(
            session.listGroups(),
            (loadedUsers, loadedGroups) -> {
              Platform.runLater(
                  () -> {
                    users.clear();
                    users.addAll(loadedUsers);
                    groups.clear();
                    groups.addAll(loadedGroups);
                    rebuildConversations();
                    setStatus("Pronto.");
                  });
              return null;
            })
        .exceptionally(
            error -> {
              Platform.runLater(() -> setStatus("Erro ao atualizar: " + rootMessage(error)));
              return null;
            });
  }

  private void rebuildConversations() {
    ConversationTarget selected = conversationList.getSelectionModel().getSelectedItem();
    conversations.setAll(
        users.stream()
            .filter(user -> !user.username().equals(session.username()))
            .map(ConversationTarget::user)
            .toList());
    conversations.addAll(groups.stream().map(ConversationTarget::group).toList());
    if (selected != null) {
      conversations.stream()
          .filter(target -> target.kind() == selected.kind() && target.id().equals(selected.id()))
          .findFirst()
          .ifPresent(conversationList.getSelectionModel()::select);
    }
  }

  private void loadHistory(ConversationTarget target) {
    messages.clear();
    sendButton.setDisable(target == null);
    if (target == null) {
      titleLabel.setText("Selecione uma conversa");
      return;
    }
    titleLabel.setText(target.label());
    setStatus("Carregando histórico...");
    session
        .history(target)
        .whenComplete(
            (history, error) ->
                Platform.runLater(
                    () -> {
                      if (error != null) {
                        setStatus("Erro ao carregar histórico: " + rootMessage(error));
                        return;
                      }
                      messages.setAll(history);
                      scrollMessages();
                      setStatus("Histórico carregado.");
                    }));
  }

  private void sendSelectedMessage() {
    ConversationTarget target = conversationList.getSelectionModel().getSelectedItem();
    String text = messageInput.getText().trim();
    if (target == null || text.isEmpty()) {
      return;
    }
    sendButton.setDisable(true);
    session
        .sendMessage(target, text)
        .whenComplete(
            (response, error) ->
                Platform.runLater(
                    () -> {
                      sendButton.setDisable(false);
                      if (error != null) {
                        setStatus("Erro ao enviar: " + rootMessage(error));
                        return;
                      }
                      if (!response.isOk()) {
                        setStatus("Erro: " + response.message());
                        return;
                      }
                      messageInput.clear();
                      loadHistory(target);
                    }));
  }

  private void createGroup() {
    Optional<GroupInput> input = showGroupDialog("Criar grupo", true);
    input.ifPresent(
        group ->
            handleGroupAction(
                session.createGroup(group.groupCode(), group.displayName()),
                response -> refreshDirectory()));
  }

  private void joinGroup() {
    askText("Entrar em grupo", "Código do grupo")
        .ifPresent(
            groupCode ->
                handleGroupAction(session.joinGroup(groupCode), response -> refreshDirectory()));
  }

  private void renameSelectedGroup() {
    selectedGroup()
        .ifPresent(
            group ->
                askText("Renomear grupo", "Novo nome")
                    .ifPresent(
                        name ->
                            handleGroupAction(
                                session.renameGroup(group.id(), name),
                                response -> refreshDirectory())));
  }

  private void leaveSelectedGroup() {
    selectedGroup()
        .ifPresent(
            group ->
                handleGroupAction(session.leaveGroup(group.id()), response -> refreshDirectory()));
  }

  private void deleteSelectedGroup() {
    selectedGroup()
        .ifPresent(
            group -> {
              Alert confirm =
                  new Alert(
                      Alert.AlertType.CONFIRMATION,
                      "Excluir " + group.label() + "?",
                      ButtonType.CANCEL,
                      ButtonType.OK);
              confirm.setHeaderText("Excluir grupo");
              if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                handleGroupAction(session.deleteGroup(group.id()), response -> refreshDirectory());
              }
            });
  }

  private void handleGroupAction(
      CompletableFuture<ServerResponse> action, Consumer<ServerResponse> onSuccess) {
    action.whenComplete(
        (response, error) ->
            Platform.runLater(
                () -> {
                  if (error != null) {
                    setStatus("Erro: " + rootMessage(error));
                    return;
                  }
                  setStatus(response.message());
                  if (response.isOk()) {
                    onSuccess.accept(response);
                  }
                }));
  }

  private Optional<ConversationTarget> selectedGroup() {
    ConversationTarget selected = conversationList.getSelectionModel().getSelectedItem();
    if (selected == null || selected.kind() != ConversationKind.GROUP) {
      setStatus("Selecione um grupo.");
      return Optional.empty();
    }
    return Optional.of(selected);
  }

  private Optional<GroupInput> showGroupDialog(String title, boolean requireDisplayName) {
    javafx.scene.control.Dialog<GroupInput> dialog = new javafx.scene.control.Dialog<>();
    dialog.setTitle(title);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

    TextField codeField = new TextField();
    codeField.setPromptText("Código do grupo");
    TextField nameField = new TextField();
    nameField.setPromptText("Nome público");
    VBox content = new VBox(10, codeField, nameField);
    content.setPadding(new Insets(10));
    dialog.getDialogPane().setContent(content);
    dialog.setResultConverter(
        button -> {
          if (button != ButtonType.OK) {
            return null;
          }
          String code = codeField.getText().trim();
          String name = nameField.getText().trim();
          if (code.isEmpty() || (requireDisplayName && name.isEmpty())) {
            return null;
          }
          return new GroupInput(code, name);
        });
    return dialog.showAndWait();
  }

  private Optional<String> askText(String title, String prompt) {
    javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
    dialog.setTitle(title);
    dialog.setHeaderText(null);
    dialog.setContentText(prompt);
    return dialog.showAndWait().map(String::trim).filter(value -> !value.isEmpty());
  }

  private void setStatus(String message) {
    if (statusLabel != null) {
      statusLabel.setText(message);
    }
  }

  private void scrollMessages() {
    if (!messages.isEmpty()) {
      messageList.scrollTo(messages.size() - 1);
    }
  }

  private void closeQuietly() {
    try {
      if (session != null) {
        session.close();
      }
    } catch (Exception ignored) {
    } finally {
      session = null;
    }
  }

  private static String rootMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
  }

  private static String stylesheet() {
    return ChatFxApplication.class.getResource("/chat-client.css").toExternalForm();
  }

  private final class FxClientListener implements ChatClientListener {

    @Override
    public void onDirectMessage(ChatMessage message) {
      Platform.runLater(
          () -> {
            appendIfCurrent(message);
            refreshDirectory();
          });
    }

    @Override
    public void onGroupMessage(ChatMessage message) {
      Platform.runLater(() -> appendIfCurrent(message));
    }

    @Override
    public void onUserOnline(ClientUser user) {
      Platform.runLater(refreshDirectoryRunnable());
    }

    @Override
    public void onUserOffline(String username) {
      Platform.runLater(refreshDirectoryRunnable());
    }

    @Override
    public void onGroupsChanged() {
      Platform.runLater(refreshDirectoryRunnable());
    }

    @Override
    public void onSystemMessage(String message) {
      Platform.runLater(() -> setStatus(message));
    }

    @Override
    public void onDisconnected() {
      Platform.runLater(
          () -> {
            setStatus("Conexão com o servidor perdida.");
            sendButton.setDisable(true);
          });
    }

    private Runnable refreshDirectoryRunnable() {
      return ChatFxApplication.this::refreshDirectory;
    }

    private void appendIfCurrent(ChatMessage message) {
      ConversationTarget selected = conversationList.getSelectionModel().getSelectedItem();
      if (selected != null
          && selected.kind() == message.kind()
          && selected.id().equals(message.conversationId())) {
        messages.add(message);
        scrollMessages();
      }
    }
  }

  private static final class ConversationCell extends ListCell<ConversationTarget> {

    @Override
    protected void updateItem(ConversationTarget item, boolean empty) {
      super.updateItem(item, empty);
      if (empty || item == null) {
        setText(null);
        return;
      }
      setText((item.kind() == ConversationKind.GROUP ? "# " : "@ ") + item.label());
    }
  }

  private static final class MessageCell extends ListCell<ChatMessage> {

    @Override
    protected void updateItem(ChatMessage item, boolean empty) {
      super.updateItem(item, empty);
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
        return;
      }
      String time = item.createdAt() != null ? TIME_FORMATTER.format(item.createdAt()) : "--:--";
      Label author = new Label("[" + time + "] " + item.authorUsername());
      author.getStyleClass().add("message-author");
      Label text = new Label(item.text());
      text.setWrapText(true);
      VBox box = new VBox(4, author, text);
      box.getStyleClass().add(item.ownMessage() ? "message-own" : "message");
      Region spacer = new Region();
      HBox row = new HBox(spacer, box);
      HBox.setHgrow(spacer, Priority.ALWAYS);
      if (!item.ownMessage()) {
        row.getChildren().setAll(box, spacer);
      }
      setGraphic(row);
    }
  }

  private record ClientConfig(String host, int port) {}

  private record GroupInput(String groupCode, String displayName) {}
}
