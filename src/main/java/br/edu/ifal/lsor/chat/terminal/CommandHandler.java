package br.edu.ifal.lsor.chat.terminal;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import br.edu.ifal.lsor.chat.socket.client.ChatClientSocket;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CommandHandler {

  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

  private final ChatClientSocket client;
  private final String username;
  private final GroupCache groupCache;
  private final TerminalEventPrinter eventPrinter;
  private final Object printLock;

  private volatile boolean closingGracefully;

  CommandHandler(
      ChatClientSocket client,
      String username,
      GroupCache groupCache,
      TerminalEventPrinter eventPrinter,
      Object printLock) {
    this.client = client;
    this.username = username;
    this.groupCache = groupCache;
    this.eventPrinter = eventPrinter;
    this.printLock = printLock;
  }

  boolean isClosingGracefully() {
    return closingGracefully;
  }

  void handle(String command, String args) throws Exception {
    switch (command) {
      case "/sair" -> cmdSair();
      case "/msg" -> cmdMsg(args);
      case "/chat" -> cmdChat(args);
      case "/list", "/listar" -> cmdList();
      case "/reply", "/responder" -> cmdReply(args);
      default -> printLine("Comando desconhecido: " + command);
    }
  }

  private void cmdSair() throws Exception {
    closingGracefully = true;
    try {
      client.send(Actions.LOGOUT, Map.of()).get(3, java.util.concurrent.TimeUnit.SECONDS);
    } catch (Exception ignored) {
    }
    try {
      client.close();
    } catch (Exception ignored) {
    }
    printLine("Desconectado.");
    System.exit(0);
  }

  private void cmdMsg(String args) throws Exception {
    String target = extractTarget(args);
    if (target == null) {
      printLine("Uso: /msg <username|groupCode> <mensagem>");
      return;
    }
    String text = args.substring(target.length()).trim();
    if (text.isEmpty()) {
      printLine("Uso: /msg <username|groupCode> <mensagem>");
      return;
    }

    if (groupCache.contains(target)) {
      sendGroupMessage(target, text);
    } else {
      sendDirectOrFallback(target, text);
    }
  }

  private void sendDirectOrFallback(String target, String text) throws Exception {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("targetUsername", target);
    payload.put("text", text);

    ServerResponse response = client.send(Actions.SEND_DIRECT, payload).get();
    if (response.isOk()) {
      printLine("Enviado para " + target + ".");
    } else if ("USER_NOT_FOUND".equals(response.code()) || "USER_OFFLINE".equals(response.code())) {
      sendGroupMessage(target, text);
    } else {
      printLine("Erro: " + response.message());
    }
  }

  private void sendGroupMessage(String groupCode, String text) throws Exception {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("groupCode", groupCode);
    payload.put("text", text);

    ServerResponse response = client.send(Actions.SEND_GROUP, payload).get();
    if (response.isOk()) {
      printLine("Enviado para #" + groupCode + ".");
    } else {
      printLine("Erro: " + response.message());
    }
  }

  private void cmdChat(String args) throws Exception {
    if (args.isEmpty()) {
      printLine("Uso: /chat <username|groupCode>");
      return;
    }
    String target = args.trim();

    if (groupCache.contains(target)) {
      fetchAndPrintHistory("GROUP", target);
      return;
    }

    Map<String, Serializable> payload = new HashMap<>();
    payload.put("scope", "DIRECT");
    payload.put("target", target);

    ServerResponse response = client.send(Actions.GET_HISTORY, payload).get();
    if (response.isOk()) {
      printHistory(response, target);
    } else if ("USER_NOT_FOUND".equals(response.code())) {
      fetchAndPrintHistory("GROUP", target);
    } else {
      printLine("Erro: " + response.message());
    }
  }

  private void fetchAndPrintHistory(String scope, String target) throws Exception {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("scope", scope);
    payload.put("target", target);

    ServerResponse response = client.send(Actions.GET_HISTORY, payload).get();
    if (!response.isOk()) {
      printLine("Erro: " + response.message());
      return;
    }
    printHistory(response, target);
  }

  private void printHistory(ServerResponse response, String target) {
    List<Map<String, Serializable>> messages =
        ChatTerminalClient.listFromPayload(response.payload(), "messages");
    if (messages.isEmpty()) {
      printLine("=== Histórico: " + target + " ===");
      printLine("(sem mensagens)");
      return;
    }

    printLine("=== Histórico: " + target + " ===");
    for (Map<String, Serializable> msg : messages) {
      String from = (String) msg.get("fromUsername");
      String text = (String) msg.get("text");
      Instant createdAt = (Instant) msg.get("createdAt");
      String time = createdAt != null ? TIME_FORMATTER.format(createdAt) : "--:--";
      printLine("[" + time + "] " + from + ": " + text);
    }
  }

  private void cmdList() throws Exception {
    printLine("=== Contatos Online ===");

    try {
      ServerResponse usersResponse =
          client.send(Actions.LIST_USERS, Map.of()).get(5, java.util.concurrent.TimeUnit.SECONDS);
      if (usersResponse.isOk()) {
        List<Map<String, Serializable>> users =
            ChatTerminalClient.listFromPayload(usersResponse.payload(), "users");
        if (users != null && !users.isEmpty()) {
          for (Map<String, Serializable> user : users) {
            String u = (String) user.get("username");
            String d = (String) user.get("displayName");
            Boolean online = (Boolean) user.get("online");
            String tag = "";
            if (u != null && u.equals(username)) {
              tag = " (você)";
            }
            String status = Boolean.TRUE.equals(online) ? " [online]" : " [offline]";
            printLine("  " + u + (d != null && !d.equals(u) ? " (" + d + ")" : "") + status + tag);
          }
        } else {
          printLine("  (nenhum usuário)");
        }
      }
    } catch (Exception exception) {
      printLine("  (erro ao carregar)");
    }

    printLine("");
    printLine("=== Grupos ===");

    try {
      ServerResponse groupsResponse =
          client.send(Actions.LIST_GROUPS, Map.of()).get(5, java.util.concurrent.TimeUnit.SECONDS);
      if (groupsResponse.isOk()) {
        List<Map<String, Serializable>> groups =
            ChatTerminalClient.listFromPayload(groupsResponse.payload(), "groups");
        if (!groups.isEmpty()) {
          groupCache.populate(groups);
          for (Map<String, Serializable> group : groups) {
            String gc = (String) group.get("groupCode");
            String gd = (String) group.get("displayName");
            String owner = (String) group.get("ownerUsername");
            Integer count = (Integer) group.get("memberCount");
            Boolean isMember = (Boolean) group.get("isMember");
            if (gc != null) {
              String memberTag = Boolean.TRUE.equals(isMember) ? " [membro]" : "";
              printLine(
                  "  #"
                      + gc
                      + (gd != null ? " - " + gd : "")
                      + " (dono: "
                      + owner
                      + ", "
                      + (count != null ? count : "?")
                      + " membros)"
                      + memberTag);
            }
          }
        } else {
          printLine("  (nenhum grupo)");
        }
      }
    } catch (Exception exception) {
      printLine("  (erro ao carregar)");
    }
  }

  private void cmdReply(String text) throws Exception {
    if (text.isEmpty()) {
      printLine("Uso: /reply <mensagem>");
      return;
    }

    if (eventPrinter.getReplyTargetGroupCode() != null) {
      sendGroupMessage(eventPrinter.getReplyTargetGroupCode(), text);
      return;
    }

    if (eventPrinter.getReplyTargetUsername() != null) {
      Map<String, Serializable> payload = new HashMap<>();
      payload.put("targetUsername", eventPrinter.getReplyTargetUsername());
      payload.put("text", text);
      ServerResponse response = client.send(Actions.SEND_DIRECT, payload).get();
      if (response.isOk()) {
        printLine("Enviado para " + eventPrinter.getReplyTargetUsername() + ".");
      } else {
        printLine("Erro: " + response.message());
      }
      return;
    }

    printLine("Nenhuma mensagem recebida para responder.");
  }

  void refreshGroupCache() {
    try {
      ServerResponse response =
          client.send(Actions.LIST_GROUPS, Map.of()).get(5, java.util.concurrent.TimeUnit.SECONDS);
      if (response.isOk()) {
        List<Map<String, Serializable>> groups =
            ChatTerminalClient.listFromPayload(response.payload(), "groups");
        if (!groups.isEmpty()) {
          groupCache.populate(groups);
        }
      }
    } catch (Exception exception) {
      printLine("Aviso: não foi possível carregar a lista de grupos.");
    }
  }

  private String extractTarget(String args) {
    int space = args.indexOf(' ');
    if (space < 0) {
      return null;
    }
    return args.substring(0, space);
  }

  private void printLine(String text) {
    synchronized (printLock) {
      System.out.println(text);
    }
  }
}
