package br.edu.ifal.lsor.chat.socket.server;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import br.edu.ifal.lsor.chat.server.ChatSession;
import br.edu.ifal.lsor.chat.server.InMemoryChatService;
import br.edu.ifal.lsor.chat.server.OutboundEvent;
import br.edu.ifal.lsor.chat.server.ServiceResult;
import br.edu.ifal.lsor.chat.socket.ChatObjectInputFilters;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ChatProtocolSocketHandler {

  private static final Logger LOGGER = LogManager.getLogger(ChatProtocolSocketHandler.class);

  private final InMemoryChatService service;
  private final ConnectionRegistry connections;

  public ChatProtocolSocketHandler(InMemoryChatService service) {
    this(service, new ConnectionRegistry());
  }

  ChatProtocolSocketHandler(InMemoryChatService service, ConnectionRegistry connections) {
    this.service = service;
    this.connections = connections;
  }

  public void handle(Socket socket) {
    String clientIp = socket.getInetAddress().getHostAddress();
    LOGGER.info("Novo cliente conectado: {}", clientIp);
    ChatSession session = new ChatSession();

    try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
      output.flush();
      ClientConnection connection = new ClientConnection(output);

      try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
        ChatObjectInputFilters.applyProtocolFilter(input);
        while (true) {
          Object object = input.readObject();
          if (!(object instanceof ClientRequest request)) {
            connection.send(
                ServerResponse.error(
                    null, Codes.INVALID_PAYLOAD, "Objeto recebido não é ClientRequest."));
            continue;
          }

          String previousUsername = session.username();
          ServiceResult result;
          synchronized (service) {
            result = service.handle(session, request);
            connections.register(session, connection);
          }

          connection.send(result.response());
          dispatch(result.events());

          if (result.closeConnection()) {
            if (previousUsername != null) {
              connections.remove(previousUsername);
            }
            break;
          }
        }
      }
    } catch (EOFException exception) {
      LOGGER.info("Conexao encerrada pelo cliente {}.", clientIp);
    } catch (Exception exception) {
      LOGGER.warn("Conexao perdida com o cliente {}.", clientIp);
    } finally {
      String username = session.username();
      if (username != null) {
        connections.remove(username);
      }
      dispatch(service.disconnect(session));
      closeSocket(socket, clientIp);
    }
  }

  void dispatch(List<OutboundEvent> events) {
    for (OutboundEvent event : events) {
      for (String username : event.targetUsernames()) {
        ClientConnection connection = connections.connection(username);
        if (connection == null) {
          continue;
        }
        try {
          connection.send(event.event());
        } catch (RuntimeException exception) {
          ChatSession disconnected = connections.remove(username, connection);
          LOGGER.warn("Falha ao enviar evento para {}.", username);
          if (disconnected != null) {
            dispatch(service.disconnect(disconnected));
          }
        }
      }
    }
  }

  private void closeSocket(Socket socket, String clientIp) {
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
        LOGGER.info("Cliente desconectado e socket fechado: {}", clientIp);
      }
    } catch (Exception exception) {
      LOGGER.error("Falha ao fechar socket do cliente.");
    }
  }
}
