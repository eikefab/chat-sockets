package br.edu.ifal.lsor.chat.terminal;

import br.edu.ifal.lsor.chat.protocol.Events;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import java.io.Serializable;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class TerminalEventPrinter {

  private static final Logger LOGGER = LogManager.getLogger(TerminalEventPrinter.class);

  private final String ownUsername;
  private final GroupCache groupCache;
  private volatile String replyTargetUsername;
  private volatile String replyTargetGroupCode;

  TerminalEventPrinter(String ownUsername, GroupCache groupCache) {
    this.ownUsername = ownUsername;
    this.groupCache = groupCache;
  }

  void handleEvent(ServerEvent event) {
    Map<String, Serializable> payload = event.payload();
    switch (event.eventType()) {
      case Events.DIRECT_MESSAGE -> {
        String from = (String) payload.get("fromUsername");
        String text = (String) payload.get("text");
        replyTargetUsername = from;
        replyTargetGroupCode = null;
        LOGGER.info("[MENSAGEM RECEBIDA] {}: {}", from, text);
      }
      case Events.GROUP_MESSAGE -> {
        String groupCode = (String) payload.get("groupCode");
        String author = (String) payload.get("authorUsername");
        String text = (String) payload.get("text");
        replyTargetGroupCode = groupCode;
        replyTargetUsername = null;
        String displayGroup = (String) payload.getOrDefault("groupDisplayName", groupCode);
        LOGGER.info("[MENSAGEM RECEBIDA] {}/{}: {}", displayGroup, author, text);
      }
      case Events.USER_ONLINE -> {
        String u = (String) payload.get("username");
        String d = (String) payload.get("displayName");
        if (u != null && !u.equals(ownUsername)) {
          String label = d != null && !d.equals(u) ? d + " (" + u + ")" : u;
          LOGGER.info("{} entrou no chat.", label);
        }
      }
      case Events.USER_OFFLINE -> {
        String u = (String) payload.get("username");
        if (u != null && !u.equals(ownUsername)) {
          LOGGER.info("{} saiu do chat.", u);
        }
      }
      case Events.GROUP_CREATED -> {
        String gc = (String) payload.get("groupCode");
        String gd = (String) payload.get("displayName");
        if (gc != null) {
          groupCache.put(gc, gd);
          LOGGER.info("Grupo criado: #{}{}", gc, gd != null ? " - " + gd : "");
        }
      }
      case Events.GROUP_RENAMED -> {
        String gc = (String) payload.get("groupCode");
        String gd = (String) payload.get("displayName");
        if (gc != null) {
          groupCache.put(gc, gd);
          LOGGER.info("Grupo renomeado: #{}{}", gc, gd != null ? " → " + gd : "");
        }
      }
      case Events.GROUP_DELETED -> {
        String gc = (String) payload.get("groupCode");
        if (gc != null) {
          groupCache.remove(gc);
          LOGGER.info("Grupo excluído: #{}", gc);
        }
      }
      case Events.GROUP_MEMBER_JOINED -> {
        String gc = (String) payload.get("groupCode");
        String member = (String) payload.get("username");
        LOGGER.info("{} entrou no grupo #{}.", member, gc);
      }
      case Events.GROUP_MEMBER_LEFT -> {
        String gc = (String) payload.get("groupCode");
        String member = (String) payload.get("username");
        LOGGER.info("{} saiu do grupo #{}.", member, gc);
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
}
