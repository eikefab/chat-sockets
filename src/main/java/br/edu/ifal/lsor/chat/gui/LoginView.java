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
  }

  void setBusy(boolean busy) {
    connectButton.setDisable(busy);
  }

  private void configureFields() {
    hostField.setPromptText("Host");
    portField.setPromptText("Porta");
    usernameField.setPromptText("Nome de usuário");
    displayNameField.setPromptText("Nome público");
    portField.setMaxWidth(100);
    statusLabel.getStyleClass().add("status");
  }

  private VBox form() {
    HBox serverFields = new HBox(8, hostField, portField);
    HBox.setHgrow(hostField, Priority.ALWAYS);
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
    return form;
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
