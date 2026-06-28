package br.edu.ifal.lsor.chat;

import java.util.Locale;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;

final class ChatCommandLine {

  static final int DEFAULT_PORT = 8080;
  static final int DEFAULT_MAX_CLIENTS = 50;
  static final String DEFAULT_SERVER_HOST = "0.0.0.0";
  static final String DEFAULT_CLIENT_HOST = "127.0.0.1";
  static final Level DEFAULT_LOG_LEVEL = Level.INFO;

  private static final Set<String> SERVER_ONLY_OPTIONS = Set.of("max-clients", "log-level");
  private static final Set<String> LOG_LEVELS = Set.of("ERROR", "WARN", "INFO", "DEBUG", "TRACE");

  private ChatCommandLine() {}

  static CliConfig parse(String[] args) throws ParseException {
    CommandLine commandLine = new DefaultParser(false).parse(options(), args);

    if (commandLine.hasOption("help")) {
      return CliConfig.forHelp();
    }
    if (commandLine.hasOption("version")) {
      return CliConfig.forVersion();
    }

    boolean server = commandLine.hasOption("server");
    boolean client = commandLine.hasOption("client");
    if (server == client) {
      throw new ParseException("Informe exatamente um modo: --server ou --client.");
    }

    if (client) {
      rejectServerOnlyOptions(commandLine);
    }

    int port = parsePort(commandLine.getOptionValue("port", String.valueOf(DEFAULT_PORT)));
    String host =
        commandLine.getOptionValue("host", server ? DEFAULT_SERVER_HOST : DEFAULT_CLIENT_HOST);
    int maxClients =
        parsePositiveInt(
            commandLine.getOptionValue("max-clients", String.valueOf(DEFAULT_MAX_CLIENTS)),
            "--max-clients");
    Level logLevel =
        parseLogLevel(commandLine.getOptionValue("log-level", DEFAULT_LOG_LEVEL.name()));

    return new CliConfig(
        server ? CliMode.SERVER : CliMode.CLIENT, host, port, maxClients, logLevel, false, false);
  }

  static void printHelp() {
    new HelpFormatter().printHelp("java -jar chat-sockets.jar", options(), true);
  }

  private static Options options() {
    Options options = new Options();
    options.addOption(Option.builder().longOpt("server").desc("executa em modo servidor").build());
    options.addOption(Option.builder().longOpt("client").desc("executa em modo cliente").build());
    options.addOption(
        Option.builder()
            .longOpt("host")
            .hasArg()
            .argName("valor")
            .desc("host de bind do servidor ou host remoto do cliente")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("port")
            .hasArg()
            .argName("valor")
            .desc("porta TCP entre 1 e 65535")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("max-clients")
            .hasArg()
            .argName("valor")
            .desc("limite de conexoes simultaneas do servidor")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("log-level")
            .hasArg()
            .argName("valor")
            .desc("nivel de log do servidor: ERROR, WARN, INFO, DEBUG ou TRACE")
            .build());
    options.addOption(Option.builder().longOpt("help").desc("mostra esta ajuda").build());
    options.addOption(Option.builder().longOpt("version").desc("mostra a versao").build());
    return options;
  }

  private static void rejectServerOnlyOptions(CommandLine commandLine) throws ParseException {
    for (String option : SERVER_ONLY_OPTIONS) {
      if (commandLine.hasOption(option)) {
        throw new ParseException("A flag --" + option + " so pode ser usada com --server.");
      }
    }
  }

  private static int parsePort(String value) throws ParseException {
    int port = parsePositiveInt(value, "--port");
    if (port > 65535) {
      throw new ParseException("A flag --port deve estar entre 1 e 65535.");
    }
    return port;
  }

  private static int parsePositiveInt(String value, String option) throws ParseException {
    try {
      int parsed = Integer.parseInt(value);
      if (parsed <= 0) {
        throw new ParseException("A flag " + option + " deve ser maior que zero.");
      }
      return parsed;
    } catch (NumberFormatException exception) {
      throw new ParseException("A flag " + option + " deve receber um numero inteiro.");
    }
  }

  private static Level parseLogLevel(String value) throws ParseException {
    String normalized = value.toUpperCase(Locale.ROOT);
    if (!LOG_LEVELS.contains(normalized)) {
      throw new ParseException("A flag --log-level deve ser ERROR, WARN, INFO, DEBUG ou TRACE.");
    }
    return Level.valueOf(normalized);
  }

  enum CliMode {
    SERVER,
    CLIENT
  }

  record CliConfig(
      CliMode mode,
      String host,
      int port,
      int maxClients,
      Level logLevel,
      boolean help,
      boolean version) {

    static CliConfig forHelp() {
      return new CliConfig(null, null, 0, 0, null, true, false);
    }

    static CliConfig forVersion() {
      return new CliConfig(null, null, 0, 0, null, false, true);
    }
  }
}
