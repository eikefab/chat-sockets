package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import java.util.Set;

public record OutboundEvent(ServerEvent event, Set<String> targetUsernames) {}
