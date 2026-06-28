package br.edu.ifal.lsor.chat.terminal;

import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import java.io.Serializable;
import java.util.Map;

final class TerminalEventPrinter {

  private final String ownUsername;
  private final GroupCache groupCache;
  private final Object printLock;
  private volatile String replyTargetUsername;
  private volatile String replyTargetGroupCode;

  TerminalEventPrinter(String ownUsername, GroupCache groupCache, Object printLock) {
    this.ownUsername = ownUsername;
    this.groupCache = groupCache;
    this.printLock = printLock;
  }

  void handleEvent(ServerEvent event) {
    Map<String, Serializable> payload = event.payload();
    switch (event.eventType()) {
      case Events.DIRECT_MESSAGE -> {
        String from = (String) payload.get("fromUsername");
        String text = (String) payload.get("text");
        replyTargetUsername = from;
        replyTargetGroupCode = null;
        printLine("[MENSAGEM RECEBIDA] " + from + ": " + text);
      }
      case Events.GROUP_MESSAGE -> {
        String groupCode = (String) payload.get("groupCode");
        String author = (String) payload.get("authorUsername");
        String text = (String) payload.get("text");
        replyTargetGroupCode = groupCode;
        replyTargetUsername = null;
        String displayGroup = (String) payload.getOrDefault("groupDisplayName", groupCode);
        printLine("[MENSAGEM RECEBIDA] " + displayGroup + "/" + author + ": " + text);
      }
      case Events.USER_ONLINE -> {
        String u = (String) payload.get("username");
        String d = (String) payload.get("displayName");
        if (u != null && !u.equals(ownUsername)) {
          String label = d != null && !d.equals(u) ? d + " (" + u + ")" : u;
          printLine(label + " entrou no chat.");
        }
      }
      case Events.USER_OFFLINE -> {
        String u = (String) payload.get("username");
        if (u != null && !u.equals(ownUsername)) {
          printLine(u + " saiu do chat.");
        }
      }
      case Events.GROUP_CREATED -> {
        String gc = (String) payload.get("groupCode");
        String gd = (String) payload.get("displayName");
        if (gc != null) {
          groupCache.put(gc, gd);
          printLine("Grupo criado: #" + gc + (gd != null ? " - " + gd : ""));
        }
      }
      case Events.GROUP_RENAMED -> {
        String gc = (String) payload.get("groupCode");
        String gd = (String) payload.get("displayName");
        if (gc != null) {
          groupCache.put(gc, gd);
          printLine("Grupo renomeado: #" + gc + (gd != null ? " → " + gd : ""));
        }
      }
      case Events.GROUP_DELETED -> {
        String gc = (String) payload.get("groupCode");
        if (gc != null) {
          groupCache.remove(gc);
          printLine("Grupo excluído: #" + gc);
        }
      }
      case Events.GROUP_MEMBER_JOINED -> {
        String gc = (String) payload.get("groupCode");
        String member = (String) payload.get("username");
        printLine(member + " entrou no grupo #" + gc + ".");
      }
      case Events.GROUP_MEMBER_LEFT -> {
        String gc = (String) payload.get("groupCode");
        String member = (String) payload.get("username");
        printLine(member + " saiu do grupo #" + gc + ".");
      }
      default -> {}
    }
  }

  String getReplyTargetUsername() {
    return replyTargetUsername;
  }

  String getReplyTargetGroupCode() {
    return replyTargetGroupCode;
  }

  private void printLine(String text) {
    synchronized (printLock) {
      System.out.println(text);
    }
  }
}
