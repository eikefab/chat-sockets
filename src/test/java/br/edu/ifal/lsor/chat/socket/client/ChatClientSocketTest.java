package br.edu.ifal.lsor.chat.socket.client;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifal.lsor.chat.protocol.Actions;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ChatClientSocketTest {

  @Test
  void closeCompletesPendingResponsesExceptionally() throws Exception {
    CountDownLatch requestRead = new CountDownLatch(1);
    try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      Thread serverThread = new Thread(() -> acceptAndReadOneRequest(server, requestRead));
      serverThread.setDaemon(true);
      serverThread.start();

      try (ChatClientSocket client = new ChatClientSocket("127.0.0.1", server.getLocalPort())) {
        client.openSocket();
        CompletableFuture<ServerResponse> pending = client.send(Actions.HEARTBEAT, Map.of());

        assertTrue(requestRead.await(3, TimeUnit.SECONDS));
        client.close();

        ExecutionException exception =
            assertThrows(ExecutionException.class, () -> pending.get(1, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
      }
      serverThread.join(1000);
    }
  }

  private static void acceptAndReadOneRequest(ServerSocket server, CountDownLatch requestRead) {
    try (Socket socket = server.accept();
        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
      output.flush();
      try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
        input.readObject();
        requestRead.countDown();
        input.readObject();
      }
    } catch (Exception ignored) {
    }
  }
}
