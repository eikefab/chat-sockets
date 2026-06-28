package br.edu.ifal.lsor.chat.socket.client;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.socket.server.ChatServerMain;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChatClientMain {

  private static final Logger LOGGER = LogManager.getLogger(ChatClientMain.class);

  public static void main(String[] args) {
    LOGGER.info("Iniciando cliente de teste...");

    try (ChatClientSocket client =
        new ChatClientSocket("127.0.0.1", ChatServerMain.SERVER_SOCKET_PORT)) {
      client.openSocket();

      LOGGER.info("Enviando LOGIN...");
      LOGGER.info(
          "Servidor respondeu: {}",
          client
              .send(
                  Actions.LOGIN,
                  Map.<String, Serializable>of(
                      "username", "cliente-teste", "displayName", "Cliente Teste"))
              .get(3, TimeUnit.SECONDS));

      LOGGER.info("Enviando HEARTBEAT...");
      LOGGER.info(
          "Servidor respondeu: {}",
          client.send(Actions.HEARTBEAT, Map.<String, Serializable>of()).get(3, TimeUnit.SECONDS));

      LOGGER.info("Enviando LOGOUT...");
      LOGGER.info(
          "Servidor respondeu: {}",
          client.send(Actions.LOGOUT, Map.<String, Serializable>of()).get(3, TimeUnit.SECONDS));

    } catch (Exception e) {
      LOGGER.error("Falha na execucao do cliente: {}", e.getMessage());
    }

    LOGGER.info("Cliente de teste finalizado.");
  }
}
