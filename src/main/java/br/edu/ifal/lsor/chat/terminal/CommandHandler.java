package br.edu.ifal.lsor.chat.terminal;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.Codes;
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

  private static final Map<String, String> ALIASES =
      Map.of(
          "/listar", "/list",
          "/responder", "/reply",
          "/?", "/help",
          "/ajuda", "/help");

  private static final String DIRECT_SCOPE = "DIRECT";
  private static final String GROUP_SCOPE = "GROUP";

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
    String canonical = ALIASES.getOrDefault(command, command);
    switch (canonical) {
      case "/sair" -> cmdSair();
      case "/msg" -> cmdMsg(args);
      case "/chat" -> cmdChat(args);
      case "/grupo" -> cmdGroup(args);
      case "/list" -> cmdList();
      case "/reply" -> cmdReply(args);
      case "/help" -> cmdHelp();
      default -> printLine("Comando desconhecido: " + command + ". Use /help para ver opções.");
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

  private void cmdHelp() {
    printLine("=== Comandos Disponíveis ===");
    printLine("  /help, /?, /ajuda               — Mostra esta ajuda");
    printLine("  /list, /listar                  — Lista contatos e grupos");
    printLine("  /msg <@usuario|#grupo> <msg>    — Envia mensagem");
    printLine("  /chat <@usuario|#grupo>         — Exibe histórico");
    printLine("  /grupo <ação> ...                — Gerencia grupos");
    printLine("  /reply, /responder <mensagem>   — Responde à última mensagem recebida");
    printLine("  /sair                           — Sai do chat");
  }

  private void cmdMsg(String args) throws Exception {
    String target = extractTarget(args);
    if (target == null) {
      printLine("Uso: /msg <@usuario|#grupo> <mensagem>");
      return;
    }
    String text = args.substring(target.length()).trim();
    if (text.isEmpty()) {
      printLine("Uso: /msg <@usuario|#grupo> <mensagem>");
      return;
    }

    ChatTarget chatTarget = parseChatTarget(target);
    if (chatTarget.scope() == TargetScope.GROUP) {
      sendGroupMessage(chatTarget.value(), text);
    } else {
      sendDirectMessage(chatTarget.value(), text);
    }
  }

  private void sendDirectMessage(String target, String text) throws Exception {
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("targetUsername", target);
    payload.put("text", text);

    ServerResponse response = client.send(Actions.SEND_DIRECT, payload).get();
    if (response.isOk()) {
      printLine("Enviado para " + target + ".");
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
      printLine("Uso: /chat <@usuario|#grupo>");
      return;
    }
    ChatTarget target = parseChatTarget(args.trim());
    fetchAndPrintHistory(target.scope().protocolValue(), target.value());
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
      sendDirectMessage(eventPrinter.getReplyTargetUsername(), text);
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

  private String firstToken(String args) {
    int space = args.indexOf(' ');
    return space < 0 ? args : args.substring(0, space);
  }

  private String stripGroupPrefix(String groupCode) {
    return groupCode.startsWith("#") ? groupCode.substring(1) : groupCode;
  }

  private ChatTarget parseChatTarget(String rawTarget) {
    if (rawTarget.startsWith("#")) {
      return new ChatTarget(TargetScope.GROUP, rawTarget.substring(1));
    }
    if (rawTarget.startsWith("@")) {
      return new ChatTarget(TargetScope.DIRECT, rawTarget.substring(1));
    }
    if (groupCache.contains(rawTarget)) {
      return new ChatTarget(TargetScope.GROUP, rawTarget);
    }
    return new ChatTarget(TargetScope.DIRECT, rawTarget);
  }

  private void cmdGroup(String args) throws Exception {
    String command = firstToken(args);
    if (command.isEmpty()) {
      printLine("Uso: /grupo <criar|entrar|sair|renomear|excluir> ...");
      return;
    }
    String rest = args.substring(command.length()).trim();
    switch (command.toLowerCase()) {
      case "criar" -> createGroup(rest);
      case "entrar" -> groupCodeAction(Actions.JOIN_GROUP, rest);
      case "sair" -> groupCodeAction(Actions.LEAVE_GROUP, rest);
      case "excluir" -> groupCodeAction(Actions.DELETE_GROUP, rest);
      case "renomear" -> renameGroup(rest);
      default -> printLine("Uso: /grupo <criar|entrar|sair|renomear|excluir> ...");
    }
  }

  private void createGroup(String args) throws Exception {
    String groupCode = extractTarget(args);
    if (groupCode == null) {
      printLine("Uso: /grupo criar <groupCode> <nome>");
      return;
    }
    String displayName = args.substring(groupCode.length()).trim();
    groupCode = stripGroupPrefix(groupCode);
    if (displayName.isEmpty()) {
      printLine("Uso: /grupo criar <groupCode> <nome>");
      return;
    }
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("groupCode", groupCode);
    payload.put("displayName", displayName);
    handleGroupResponse(client.send(Actions.CREATE_GROUP, payload).get(), groupCode, displayName);
  }

  private void renameGroup(String args) throws Exception {
    String groupCode = extractTarget(args);
    if (groupCode == null) {
      printLine("Uso: /grupo renomear <groupCode> <novo nome>");
      return;
    }
    String displayName = args.substring(groupCode.length()).trim();
    groupCode = stripGroupPrefix(groupCode);
    if (displayName.isEmpty()) {
      printLine("Uso: /grupo renomear <groupCode> <novo nome>");
      return;
    }
    Map<String, Serializable> payload = new HashMap<>();
    payload.put("groupCode", groupCode);
    payload.put("displayName", displayName);
    handleGroupResponse(client.send(Actions.RENAME_GROUP, payload).get(), groupCode, displayName);
  }

  private void groupCodeAction(String action, String groupCode) throws Exception {
    groupCode = stripGroupPrefix(groupCode);
    if (groupCode.isEmpty()) {
      printLine("Informe o código do grupo.");
      return;
    }
    ServerResponse response = client.send(action, Map.of("groupCode", groupCode)).get();
    handleGroupResponse(response, groupCode, groupCache.displayName(groupCode));
  }

  private void handleGroupResponse(ServerResponse response, String groupCode, String displayName) {
    if (!response.isOk()) {
      printLine("Erro: " + response.message());
      return;
    }
    if (Codes.GROUP_DELETED.equals(response.code()) || Codes.GROUP_LEFT.equals(response.code())) {
      groupCache.remove(groupCode);
    } else {
      groupCache.put(groupCode, displayName);
    }
    printLine(response.message());
  }

  private void printLine(String text) {
    synchronized (printLock) {
      System.out.println(text);
    }
  }

  private enum TargetScope {
    DIRECT(DIRECT_SCOPE),
    GROUP(GROUP_SCOPE);

    private final String protocolValue;

    TargetScope(String protocolValue) {
      this.protocolValue = protocolValue;
    }

    String protocolValue() {
      return protocolValue;
    }
  }

  private record ChatTarget(TargetScope scope, String value) {}
}
