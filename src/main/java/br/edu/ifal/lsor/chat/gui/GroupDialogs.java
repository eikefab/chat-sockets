package br.edu.ifal.lsor.chat.gui;

import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

final class GroupDialogs {

  private GroupDialogs() {}

  static Optional<GroupInput> createGroup() {
    Dialog<GroupInput> dialog = new Dialog<>();
    dialog.setTitle("Criar grupo");
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

    TextField codeField = new TextField();
    codeField.setPromptText("Código do grupo");
    TextField nameField = new TextField();
    nameField.setPromptText("Nome público");
    VBox content = new VBox(10, codeField, nameField);
    content.setPadding(new Insets(10));
    dialog.getDialogPane().setContent(content);
    dialog.setResultConverter(
        button -> {
          if (button != ButtonType.OK) {
            return null;
          }
          String code = codeField.getText().trim();
          String name = nameField.getText().trim();
          if (code.isEmpty() || name.isEmpty()) {
            return null;
          }
          return new GroupInput(code, name);
        });
    return dialog.showAndWait();
  }

  static Optional<String> askGroupCode() {
    return askText("Entrar em grupo", "Código do grupo");
  }

  static Optional<String> askNewName() {
    return askText("Renomear grupo", "Novo nome");
  }

  static boolean confirmDelete(String groupLabel) {
    Alert confirm =
        new Alert(
            Alert.AlertType.CONFIRMATION,
            "Excluir " + groupLabel + "?",
            ButtonType.CANCEL,
            ButtonType.OK);
    confirm.setHeaderText("Excluir grupo");
    return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
  }

  private static Optional<String> askText(String title, String prompt) {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle(title);
    dialog.setHeaderText(null);
    dialog.setContentText(prompt);
    return dialog.showAndWait().map(String::trim).filter(value -> !value.isEmpty());
  }

  record GroupInput(String groupCode, String displayName) {}
}
