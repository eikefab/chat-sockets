package br.edu.ifal.lsor.chat.gui;

import javafx.application.Platform;

final class FxDispatch {

  private FxDispatch() {}

  static void run(Runnable action) {
    if (Platform.isFxApplicationThread()) {
      action.run();
    } else {
      Platform.runLater(action);
    }
  }

  static String rootMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
  }
}
