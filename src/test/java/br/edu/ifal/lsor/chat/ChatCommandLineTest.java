package br.edu.ifal.lsor.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifal.lsor.chat.ChatCommandLine.CliConfig;
import br.edu.ifal.lsor.chat.ChatCommandLine.CliMode;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

class ChatCommandLineTest {

  @Test
  void parsesServerOptions() throws Exception {
    CliConfig config =
        ChatCommandLine.parse(
            new String[] {
              "--server",
              "--host",
              "127.0.0.1",
              "--port",
              "9090",
              "--max-clients",
              "10",
              "--log-level",
              "DEBUG"
            });

    assertEquals(CliMode.SERVER, config.mode());
    assertEquals("127.0.0.1", config.host());
    assertEquals(9090, config.port());
    assertEquals(10, config.maxClients());
    assertEquals(Level.DEBUG, config.logLevel());
  }

  @Test
  void parsesClientOptionsWithDefaults() throws Exception {
    CliConfig config = ChatCommandLine.parse(new String[] {"--client"});

    assertEquals(CliMode.CLIENT, config.mode());
    assertEquals(ChatCommandLine.DEFAULT_CLIENT_HOST, config.host());
    assertEquals(ChatCommandLine.DEFAULT_PORT, config.port());
  }

  @Test
  void parsesServerOptionsWithDefaults() throws Exception {
    CliConfig config = ChatCommandLine.parse(new String[] {"--server"});

    assertEquals(CliMode.SERVER, config.mode());
    assertEquals(ChatCommandLine.DEFAULT_SERVER_HOST, config.host());
    assertEquals(ChatCommandLine.DEFAULT_PORT, config.port());
    assertEquals(ChatCommandLine.DEFAULT_MAX_CLIENTS, config.maxClients());
    assertEquals(ChatCommandLine.DEFAULT_LOG_LEVEL, config.logLevel());
  }

  @Test
  void rejectsDuplicatedModes() {
    ParseException exception =
        assertThrows(
            ParseException.class,
            () -> ChatCommandLine.parse(new String[] {"--server", "--client"}));

    assertTrue(exception.getMessage().contains("exatamente um modo"));
  }

  @Test
  void rejectsInvalidPort() {
    ParseException exception =
        assertThrows(
            ParseException.class,
            () -> ChatCommandLine.parse(new String[] {"--server", "--port", "70000"}));

    assertTrue(exception.getMessage().contains("--port"));
  }

  @Test
  void rejectsInvalidMaxClients() {
    ParseException exception =
        assertThrows(
            ParseException.class,
            () -> ChatCommandLine.parse(new String[] {"--server", "--max-clients", "0"}));

    assertTrue(exception.getMessage().contains("--max-clients"));
  }

  @Test
  void rejectsServerOnlyOptionsInClientMode() {
    ParseException exception =
        assertThrows(
            ParseException.class,
            () -> ChatCommandLine.parse(new String[] {"--client", "--log-level", "WARN"}));

    assertTrue(exception.getMessage().contains("--log-level"));
  }
}
