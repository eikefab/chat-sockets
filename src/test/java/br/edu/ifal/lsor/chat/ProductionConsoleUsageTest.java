package br.edu.ifal.lsor.chat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProductionConsoleUsageTest {

  private static final Pattern CONSOLE_WRITE =
      Pattern.compile("\\bSystem\\.(?:out|err)\\.(?:print|println|printf)\\b");

  @Test
  void productionCodeDoesNotWriteDirectlyToConsole() throws IOException {
    Path sourceRoot = Path.of("src/main/java");
    List<String> matches = new ArrayList<>();

    try (Stream<Path> files = Files.walk(sourceRoot)) {
      for (Path file :
          files.filter(Files::isRegularFile).filter(ProductionConsoleUsageTest::isJava).toList()) {
        List<String> lines = Files.readAllLines(file);
        for (int index = 0; index < lines.size(); index++) {
          String line = lines.get(index);
          if (CONSOLE_WRITE.matcher(line).find()) {
            matches.add(sourceRoot.relativize(file) + ":" + (index + 1) + " " + line.trim());
          }
        }
      }
    }

    assertTrue(
        matches.isEmpty(),
        "Use Log4j2 instead of direct console writes: " + String.join(", ", matches));
  }

  private static boolean isJava(Path file) {
    return file.toString().endsWith(".java");
  }
}
