package br.edu.ifal.lsor.chat.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class LoginView {

  private final TextField hostField;
  private final TextField portField;
  private final TextField usernameField = new TextField();
  private final TextField displayNameField = new TextField();
  private final Button connectButton = new Button("Entrar");
  private final Label statusLabel = new Label();
  private final BorderPane root;

  LoginView(String host, int port, Runnable submitAction) {
    this.hostField = new TextField(host);
    this.portField = new TextField(String.valueOf(port));
    configureFields();
    connectButton.setOnAction(event -> submitAction.run());
    connectButton.getStyleClass().add("primary");
    connectButton.setGraphic(Icons.login());
    displayNameField.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER) {
            submitAction.run();
          }
        });
    root = new BorderPane(form());
    root.getStyleClass().add("root");
  }

  Parent root() {
    return root;
  }

  LoginRequest request() {
    return new LoginRequest(
        hostField.getText(),
        portField.getText(),
        usernameField.getText(),
        displayNameField.getText());
  }

  void setStatus(String status) {
    statusLabel.setText(status);
    statusLabel.getStyleClass().remove("error");
    if (!status.isEmpty()) {
      statusLabel.getStyleClass().add("error");
    }
  }

  void setBusy(boolean busy) {
    connectButton.setDisable(busy);
  }

  private void configureFields() {
    hostField.setPromptText("Host");
    portField.setPromptText("Porta");
    usernameField.setPromptText("Nome de usuário");
    displayNameField.setPromptText("Nome público (opcional)");
    portField.setMaxWidth(120);
    statusLabel.getStyleClass().add("status");
    statusLabel.setMinHeight(18);
  }

  private VBox form() {
    Label brand = new Label("Chat IFAL");
    brand.getStyleClass().add("login-brand");
    brand.setGraphic(Icons.chat());
    brand.setGraphicTextGap(10);
    Label sub = new Label("Conecte-se ao servidor para iniciar uma conversa.");
    sub.getStyleClass().add("login-sub");

    VBox serverFields = new VBox(6, fieldLabel("Servidor"), new HBox(8, hostField, portField));
    HBox.setHgrow(hostField, Priority.ALWAYS);
    VBox usernameFields = new VBox(6, fieldLabel("Usuário"), usernameField);
    VBox displayFields = new VBox(6, fieldLabel("Nome público"), displayNameField);

    VBox form =
        new VBox(
            18,
            new VBox(2, brand, sub),
            serverFields,
            usernameFields,
            displayFields,
            connectButton,
            statusLabel);
    form.setAlignment(Pos.TOP_CENTER);
    form.setMaxWidth(380);
    form.getStyleClass().add("login-card");
    BorderPane.setMargin(form, new Insets(40));
    BorderPane.setAlignment(form, Pos.CENTER);
    return form;
  }

  private static Label fieldLabel(String text) {
    Label label = new Label(text);
    label.getStyleClass().add("field-label");
    label.setPadding(new Insets(0, 0, 0, 2));
    return label;
  }

  record LoginRequest(String host, String port, String username, String displayName) {

    String cleanUsername() {
      return username.trim();
    }

    String cleanDisplayName() {
      String user = cleanUsername();
      String display = displayName.trim();
      return display.isEmpty() ? user : display;
    }

    int cleanPort() {
      return Integer.parseInt(port.trim());
    }
  }
}
