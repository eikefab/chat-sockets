package br.edu.ifal.lsor.chat.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import br.edu.ifal.lsor.chat.protocol.Protocol;
import br.edu.ifal.lsor.chat.protocol.ServerEvent;
import br.edu.ifal.lsor.chat.protocol.ServerResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatObjectInputFiltersTest {

  @Test
  void acceptsClientRequestWithProtocolPayloadValues() throws Exception {
    ClientRequest request =
        ClientRequest.of(
            "LIST_GROUPS",
            Map.<String, Serializable>of("username", "maria", "onlyMine", true, "limit", 50));

    ClientRequest decoded = readFiltered(request, ClientRequest.class);

    assertEquals(request.action(), decoded.action());
    assertEquals("maria", decoded.payload().get("username"));
    assertEquals(true, decoded.payload().get("onlyMine"));
    assertEquals(50, decoded.payload().get("limit"));
  }

  @Test
  void acceptsServerResponseWithNestedHistoryPayload() throws Exception {
    UUID messageId = UUID.randomUUID();
    Instant createdAt = Instant.now();
    ServerResponse response =
        ServerResponse.ok(
            UUID.randomUUID(),
            "HISTORY_RETURNED",
            "Histórico retornado.",
            Map.of(
                "scope",
                "DIRECT",
                "target",
                "joao",
                "messages",
                (Serializable)
                    List.of(
                        Map.of(
                            "messageId",
                            messageId,
                            "fromUsername",
                            "maria",
                            "text",
                            "Oi",
                            "createdAt",
                            createdAt))));

    ServerResponse decoded = readFiltered(response, ServerResponse.class);

    assertEquals(Protocol.VERSION, decoded.protocolVersion());
    assertEquals("HISTORY_RETURNED", decoded.code());
    assertEquals("DIRECT", decoded.payload().get("scope"));
  }

  @Test
  void acceptsServerEventWithProtocolPayloadValues() throws Exception {
    UUID messageId = UUID.randomUUID();
    Instant createdAt = Instant.now();
    ServerEvent event =
        ServerEvent.of(
            "DIRECT_MESSAGE",
            Map.<String, Serializable>of(
                "messageId",
                messageId,
                "fromUsername",
                "maria",
                "text",
                "Oi",
                "createdAt",
                createdAt));

    ServerEvent decoded = readFiltered(event, ServerEvent.class);

    assertEquals("DIRECT_MESSAGE", decoded.eventType());
    assertEquals(messageId, decoded.payload().get("messageId"));
  }

  @Test
  void rejectsClassesOutsideProtocolAllowlist() throws Exception {
    byte[] serialized = serialize(BigInteger.valueOf(42));

    assertThrows(InvalidClassException.class, () -> readFiltered(serialized));
  }

  @Test
  void rejectsClientRequestPayloadValuesOutsideProtocolAllowlist() throws Exception {
    ClientRequest request =
        ClientRequest.of("LOGIN", Map.<String, Serializable>of("number", BigInteger.valueOf(42)));
    byte[] serialized = serialize(request);

    assertThrows(InvalidClassException.class, () -> readFiltered(serialized));
  }

  @Test
  void acceptsManyMessagesOverASingleLongLivedStream() throws Exception {
    int messageCount = 500;
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
      for (int i = 0; i < messageCount; i++) {
        ChatObjectStreams.writeAndReset(
            output,
            ServerEvent.of(
                "DIRECT_MESSAGE",
                Map.<String, Serializable>of(
                    "messageId",
                    UUID.randomUUID(),
                    "fromUsername",
                    "maria",
                    "text",
                    "Mensagem " + i,
                    "createdAt",
                    Instant.now())));
      }
    }

    int decoded = 0;
    try (ObjectInputStream input =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      input.setObjectInputFilter(ChatObjectInputFilters.protocolFilter());
      for (int i = 0; i < messageCount; i++) {
        Object object = input.readObject();
        assertEquals(ServerEvent.class, object.getClass());
        decoded++;
      }
    }

    assertEquals(messageCount, decoded);
  }

  private static <T> T readFiltered(Serializable object, Class<T> expectedType) throws Exception {
    return expectedType.cast(readFiltered(serialize(object)));
  }

  private static Object readFiltered(byte[] serialized) throws Exception {
    try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
      input.setObjectInputFilter(ChatObjectInputFilters.protocolFilter());
      return input.readObject();
    }
  }

  private static byte[] serialize(Serializable object) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
      output.writeObject(object);
    }
    return bytes.toByteArray();
  }
}
