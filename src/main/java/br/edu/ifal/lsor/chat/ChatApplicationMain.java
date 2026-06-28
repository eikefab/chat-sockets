package br.edu.ifal.lsor.chat;

import br.edu.ifal.lsor.chat.ChatCommandLine.CliConfig;
import br.edu.ifal.lsor.chat.server.InMemoryChatService;
import br.edu.ifal.lsor.chat.socket.server.ChatProtocolSocketHandler;
import br.edu.ifal.lsor.chat.socket.server.ChatServer;
import br.edu.ifal.lsor.chat.terminal.ChatTerminalClient;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public final class ChatApplicationMain {

  private static final Logger LOGGER = LogManager.getLogger(ChatApplicationMain.class);

  private ChatApplicationMain() {}

  public static void main(String[] args) {
    try {
      CliConfig config = ChatCommandLine.parse(args);
      if (config.help()) {
        ChatCommandLine.printHelp();
        return;
      }
      if (config.version()) {
        System.out.println(version());
        return;
      }
      switch (config.mode()) {
        case SERVER -> runServer(config);
        case CLIENT -> runClient(config);
      }
    } catch (ParseException exception) {
      System.err.println("Erro nos argumentos: " + exception.getMessage());
      ChatCommandLine.printHelp();
      System.exit(2);
    } catch (IllegalStateException exception) {
      Throwable rootCause = rootCause(exception);
      LOGGER.error("Falha ao executar aplicacao: {}", rootCause.getMessage());
      System.exit(1);
    } catch (Exception exception) {
      LOGGER.error("Falha ao executar aplicacao: {}", exception.getMessage());
      System.exit(1);
    }
  }

  private static void runServer(CliConfig config) {
    Configurator.setRootLevel(config.logLevel());

    ChatProtocolSocketHandler handler = new ChatProtocolSocketHandler(new InMemoryChatService());
    ChatServer server =
        new ChatServer(config.host(), config.port(), config.maxClients(), handler::handle);
    Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer, "chat-server-shutdown"));

    server.initServer();
  }

  private static void runClient(CliConfig config) {
    ChatTerminalClient terminal = new ChatTerminalClient(config.host(), config.port());
    terminal.start();
  }

  private static String version() {
    String version = ChatApplicationMain.class.getPackage().getImplementationVersion();
    return version == null ? "1.0-SNAPSHOT" : version;
  }

  private static Throwable rootCause(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current;
  }
}
