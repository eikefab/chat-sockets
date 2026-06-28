package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;

interface ChatAction {
  ServiceResult handle(ChatSession session, ClientRequest request, PayloadReader payload)
      throws InvalidPayloadException, ServiceFailureException;
}
