package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import java.util.List;

public class InMemoryChatService {

  private final ChatActionHandler actions = new ChatActionHandler();

  public synchronized ServiceResult handle(ChatSession session, ClientRequest request) {
    return actions.handle(session, request);
  }

  public synchronized List<OutboundEvent> disconnect(ChatSession session) {
    return actions.disconnect(session);
  }
}
