package br.edu.ifal.lsor.chat.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class ChatFxApplication extends Application {

  private static final String HOST_PARAM = "--host=";
  private static final String PORT_PARAM = "--port=";

  private Stage stage;
  private ChatViewModel model;
  private LoginView loginView;
  private ClientConfig config;

  public static void launchClient(String host, int port) {
    launch(ChatFxApplication.class, HOST_PARAM + host, PORT_PARAM + port);
  }

  @Override
  public void start(Stage primaryStage) {
    this.stage = primaryStage;
    loadInterFonts();
    this.config = configFromParameters();
    stage.setTitle("Chat IFAL");
    stage.setMinWidth(920);
    stage.setMinHeight(620);
    showLogin(config);
    stage.show();
  }

  @Override
  public void stop() throws Exception {
    if (model != null) {
      model.close();
    }
  }

  private void showLogin(ClientConfig config) {
    loginView = new LoginView(config.host(), config.port(), this::submitLogin);
    setScene(loginView.root(), 920, 620);
  }

  private void submitLogin() {
    LoginView.LoginRequest request = loginView.request();
    String username = request.cleanUsername();
    if (username.isEmpty()) {
      loginView.setStatus("Informe o nome de usuário.");
      return;
    }

    int port;
    try {
      port = request.cleanPort();
    } catch (NumberFormatException exception) {
      loginView.setStatus("Porta inválida.");
      return;
    }

    loginView.setBusy(true);
    model = new ChatViewModel(request.host().trim(), port);
    model.connectAndLogin(
        username,
        request.cleanDisplayName(),
        response -> showChat(),
        message -> {
          loginView.setBusy(false);
          loginView.setStatus(message);
          model = null;
        });
  }

  private void showChat() {
    ChatMainView view = new ChatMainView(model, this::backToLogin);
    setScene(view.root(), 1040, 680);
  }

  private void backToLogin() {
    if (model != null) {
      try {
        model.close();
      } catch (Exception ignored) {
      }
      model = null;
    }
    showLogin(config);
  }

  private void setScene(javafx.scene.Parent root, int width, int height) {
    Scene scene = new Scene(root, width, height);
    scene.getStylesheets().add(stylesheet());
    stage.setScene(scene);
  }

  private ClientConfig configFromParameters() {
    String host = "127.0.0.1";
    int port = 8080;
    for (String argument : getParameters().getRaw()) {
      if (argument.startsWith(HOST_PARAM)) {
        host = argument.substring(HOST_PARAM.length());
      } else if (argument.startsWith(PORT_PARAM)) {
        port = Integer.parseInt(argument.substring(PORT_PARAM.length()));
      }
    }
    return new ClientConfig(host, port);
  }

  private static String stylesheet() {
    return ChatFxApplication.class.getResource("/chat-client.css").toExternalForm();
  }

  private static void loadInterFonts() {
    loadFont("/fonts/Inter-Regular.ttf");
    loadFont("/fonts/Inter-SemiBold.ttf");
  }

  private static void loadFont(String resource) {
    java.net.URL url = ChatFxApplication.class.getResource(resource);
    if (url != null) {
      Font.loadFont(url.toExternalForm(), 14);
    }
  }

  private record ClientConfig(String host, int port) {}
}
