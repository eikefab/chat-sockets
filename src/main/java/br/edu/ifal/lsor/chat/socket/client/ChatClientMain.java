package br.edu.ifal.lsor.chat.socket.client;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.socket.server.ChatServerMain;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChatClientMain {

  public static void main(String[] args) {
    System.out.println("[SISTEMA] Iniciando cliente de teste...");

    try (ChatClientSocket client =
        new ChatClientSocket("127.0.0.1", ChatServerMain.SERVER_SOCKET_PORT)) {
      client.openSocket();

      System.out.println("[CLIENTE] Enviando LOGIN...");
      System.out.println(
          "[SERVIDOR RESPONDEU]: "
              + client
                  .send(
                      Actions.LOGIN,
                      Map.<String, Serializable>of(
                          "username", "cliente-teste",
                          "displayName", "Cliente Teste"))
                  .get(3, TimeUnit.SECONDS));

      System.out.println("[CLIENTE] Enviando HEARTBEAT...");
      System.out.println(
          "[SERVIDOR RESPONDEU]: "
              + client
                  .send(Actions.HEARTBEAT, Map.<String, Serializable>of())
                  .get(3, TimeUnit.SECONDS));

      System.out.println("[CLIENTE] Enviando LOGOUT...");
      System.out.println(
          "[SERVIDOR RESPONDEU]: "
              + client
                  .send(Actions.LOGOUT, Map.<String, Serializable>of())
                  .get(3, TimeUnit.SECONDS));

    } catch (Exception e) {
      System.err.println("[ERRO CRÍTICO] Falha na execução do cliente: " + e.getMessage());
    }

    System.out.println("[SISTEMA] Cliente de teste finalizado.");
  }
}
