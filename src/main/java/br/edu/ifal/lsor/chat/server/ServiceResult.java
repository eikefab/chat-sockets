package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.util.List;

public record ServiceResult(
    ServerResponse response, List<OutboundEvent> events, boolean closeConnection) {

  public ServiceResult(ServerResponse response) {
    this(response, List.of(), false);
  }
}
