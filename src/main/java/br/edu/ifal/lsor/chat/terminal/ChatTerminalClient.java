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

public final class ChatTerminalClient {

  private final String host;
  private final int port;
  private ChatClientSocket client;
  private String username;
  private String displayName;

  private final GroupCache groupCache = new GroupCache();
  private final Object printLock = new Object();
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
      printLine("Falha ao conectar ao servidor: " + exception.getMessage());
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

    eventPrinter = new TerminalEventPrinter(username, groupCache, printLock);
    commandHandler = new CommandHandler(client, username, groupCache, eventPrinter, printLock);

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

    printLine("=== Chat v1.0 ===");
    printLine(
        "Conectado como "
            + username
            + (displayName.equals(username) ? "" : " (" + displayName + ")"));
    printLine("Digite /list para ver contatos e grupos. /sair para sair.");

    commandHandler.refreshGroupCache();

    commandLoop();
  }

  private String readLine() {
    if (!scanner.hasNextLine()) {
      printLine("\nEntrada encerrada.");
      System.exit(0);
      return "";
    }
    return scanner.nextLine();
  }

  private boolean doLogin() {
    print("Nome de usuário: ");
    String user = readLine().trim();
    if (user.isEmpty()) {
      printLine("Nome de usuário não pode ser vazio.");
      return false;
    }

    print("Nome público (ENTER para usar '" + user + "'): ");
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
        return true;
      }
      printLine("Falha no login: " + response.message());
      return false;
    } catch (Exception exception) {
      printLine("Falha ao realizar login: " + exception.getMessage());
      return false;
    }
  }

  private void commandLoop() {
    while (true) {
      print("> ");
      String line = readLine();

      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }

      if (!line.startsWith("/")) {
        printLine("Comando desconhecido. Use /list para ver opções.");
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
        printLine("Erro: " + exception.getMessage());
      }
    }
  }

  private void handleEvent(ServerEvent event) {
    eventPrinter.handleEvent(event);
  }

  private void handleDisconnect() {
    if (commandHandler != null && commandHandler.isClosingGracefully()) {
      return;
    }
    if (username != null) {
      printLine("\nConexão com o servidor perdida.");
    }
    System.exit(1);
  }

  private void print(String text) {
    synchronized (printLock) {
      System.out.print(text);
    }
  }

  private void printLine(String text) {
    synchronized (printLock) {
      System.out.println(text);
    }
  }
}
