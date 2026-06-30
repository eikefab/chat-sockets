package br.edu.ifal.lsor.chat.terminal;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import br.edu.ifal.lsor.chat.socket.client.ChatClientSocket;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ChatTerminalClient {

  private static final Logger LOGGER = LogManager.getLogger(ChatTerminalClient.class);

  private final String host;
  private final int port;
  private ChatClientSocket client;
  private String username;
  private String displayName;

  private final GroupCache groupCache = new GroupCache();
  private final Scanner scanner = new Scanner(System.in);

  private TerminalEventPrinter eventPrinter;
  private CommandHandler commandHandler;

  @SuppressWarnings("unchecked")
  static List<Map<String, Serializable>> listFromPayload(
      Map<String, Serializable> payload, String key) {
    Serializable value = payload.get(key);
    if (value instanceof List<?> list) {
      return (List<Map<String, Serializable>>) list;
    }
    return List.of();
  }

  public ChatTerminalClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void start() {
    client = new ChatClientSocket(host, port, this::handleEvent, this::handleDisconnect);

    try {
      client.openSocket();
    } catch (Exception exception) {
      LOGGER.error("Falha ao conectar ao servidor: {}", exception.getMessage());
      System.exit(1);
      return;
    }

    if (!doLogin()) {
      try {
        client.close();
      } catch (Exception ignored) {
      }
      return;
    }

    commandHandler = new CommandHandler(client, username, groupCache, eventPrinter);

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    client.close();
                  } catch (Exception ignored) {
                  }
                },
                "chat-terminal-shutdown"));

    LOGGER.info("=== Chat v1.0 ===");
    LOGGER.info(
        "Conectado como "
            + username
            + (displayName.equals(username) ? "" : " (" + displayName + ")"));
    LOGGER.info("Digite /list para ver contatos e grupos. /sair para sair.");

    commandHandler.refreshGroupCache();

    commandLoop();
  }

  private String readLine() {
    if (!scanner.hasNextLine()) {
      LOGGER.info("Entrada encerrada.");
      System.exit(0);
      return "";
    }
    return scanner.nextLine();
  }

  private boolean doLogin() {
    LOGGER.info("Nome de usuário: ");
    String user = readLine().trim();
    if (user.isEmpty()) {
      LOGGER.warn("Nome de usuário não pode ser vazio.");
      return false;
    }

    LOGGER.info("Nome público (ENTER para usar '{}'): ", user);
    String display = readLine().trim();
    if (display.isEmpty()) {
      display = user;
    }

    Map<String, Serializable> payload = new HashMap<>();
    payload.put("username", user);
    payload.put("displayName", display);

    try {
      ServerResponse response = client.send(Actions.LOGIN, payload).get();
      if (response.isOk()) {
        this.username = user;
        this.displayName = display;
        this.eventPrinter = new TerminalEventPrinter(username, groupCache);
        return true;
      }
      LOGGER.error("Falha no login: {}", response.message());
      return false;
    } catch (Exception exception) {
      LOGGER.error("Falha ao realizar login: {}", exception.getMessage());
      return false;
    }
  }

  private void commandLoop() {
    while (true) {
      LOGGER.info("> ");
      String line = readLine();

      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }

      if (!line.startsWith("/")) {
        LOGGER.warn("Comando desconhecido. Use /list para ver opções.");
        continue;
      }

      int firstSpace = line.indexOf(' ');
      String command;
      String args;
      if (firstSpace < 0) {
        command = line.toLowerCase();
        args = "";
      } else {
        command = line.substring(0, firstSpace).toLowerCase();
        args = line.substring(firstSpace + 1).trim();
      }

      try {
        commandHandler.handle(command, args);
      } catch (Exception exception) {
        LOGGER.error("Erro: {}", exception.getMessage());
      }
    }
  }

  private void handleEvent(ServerEvent event) {
    if (eventPrinter != null) {
      eventPrinter.handleEvent(event);
    }
  }

  private void handleDisconnect() {
    if (commandHandler != null && commandHandler.isClosingGracefully()) {
      return;
    }
    if (username != null) {
      LOGGER.error("Conexão com o servidor perdida.");
    }
    System.exit(1);
  }
}
