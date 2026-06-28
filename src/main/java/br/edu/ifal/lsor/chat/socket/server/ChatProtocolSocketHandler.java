package br.edu.ifal.lsor.chat.socket.server;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.Codes;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import br.edu.ifal.lsor.chat.server.ChatSession;
import br.edu.ifal.lsor.chat.server.InMemoryChatService;
import br.edu.ifal.lsor.chat.server.OutboundEvent;
import br.edu.ifal.lsor.chat.server.ServiceResult;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ChatProtocolSocketHandler {

  private final InMemoryChatService service;
  private final ConcurrentMap<String, ClientConnection> connections;

  public ChatProtocolSocketHandler(InMemoryChatService service) {
    this(service, new ConcurrentHashMap<>());
  }

  ChatProtocolSocketHandler(
      InMemoryChatService service, ConcurrentMap<String, ClientConnection> connections) {
    this.service = service;
    this.connections = connections;
  }

  public void handle(Socket socket) {
    String clientIp = socket.getInetAddress().getHostAddress();
    System.out.println("[LOG] Novo cliente conectado: " + clientIp);
    ChatSession session = new ChatSession();

    try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
      output.flush();
      ClientConnection connection = new ClientConnection(output);

      try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
        while (true) {
          Object object = input.readObject();
          if (!(object instanceof ClientRequest request)) {
            connection.send(
                ServerResponse.error(
                    null, Codes.INVALID_PAYLOAD, "Objeto recebido não é ClientRequest."));
            continue;
          }

          String previousUsername = session.username();
          ServiceResult result = service.handle(session, request);
          if (session.isAuthenticated()) {
            connections.put(session.username(), connection);
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
      System.err.println("[AVISO] Conexão encerrada pelo cliente " + clientIp + ".");
    } catch (Exception exception) {
      System.err.println("[AVISO] Conexão perdida com o cliente " + clientIp + ".");
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
        ClientConnection connection = connections.get(username);
        if (connection == null) {
          continue;
        }
        try {
          connection.send(event.event());
        } catch (RuntimeException exception) {
          connections.remove(username, connection);
          System.err.println("[AVISO] Falha ao enviar evento para " + username + ".");
        }
      }
    }
  }

  private void closeSocket(Socket socket, String clientIp) {
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
        System.out.println("[LOG] Cliente desconectado e socket fechado: " + clientIp);
      }
    } catch (Exception exception) {
      System.err.println("[ERRO] Falha ao fechar socket do cliente.");
    }
  }
}
