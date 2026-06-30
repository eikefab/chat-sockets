package br.edu.ifal.lsor.chat.gui;

import java.util.Optional;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

final class GroupDialogs {

  private GroupDialogs() {}

  static Optional<GroupInput> createGroup() {
    Dialog<GroupInput> dialog = new Dialog<>();
    dialog.setTitle("Criar grupo");
    dialog.setHeaderText(null);
    DialogPane pane = dialog.getDialogPane();
    pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

    TextField codeField = new TextField();
    codeField.setPromptText("ex.: lab-redes-2026");
    TextField nameField = new TextField();
    nameField.setPromptText("ex.: Laboratório de Redes");

    VBox content =
        new VBox(
            header(
                Icons.accountGroup(),
                "Criar grupo",
                "Crie um espaço compartilhado para conversar",
                false),
            fieldGroup("Código", codeField, "Identificador único usado para entrar no grupo"),
            fieldGroup("Nome público", nameField, "Nome exibido para todos os participantes"));
    content.getStyleClass().add("dialog-body");
    pane.setContent(content);

    Button ok = primaryButton(pane, "Criar grupo", Icons.plus());
    ok.disableProperty()
        .bind(codeField.textProperty().isEmpty().or(nameField.textProperty().isEmpty()));

    applyTheme(pane);
    Platform.runLater(codeField::requestFocus);

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
    return askText(
        "Entrar em grupo",
        Icons.importIcon(),
        "Entrar em grupo",
        "Use o código compartilhado pelo grupo",
        "Código do grupo",
        "Peça o código a um membro do grupo",
        "Entrar",
        Icons.importIcon());
  }

  static Optional<String> askNewName() {
    return askText(
        "Renomear grupo",
        Icons.accountGroup(),
        "Renomear grupo",
        "Atualize o nome exibido",
        "Novo nome",
        "Nome exibido para todos os participantes",
        "Salvar",
        null);
  }

  static boolean confirmDelete(String groupLabel) {
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Excluir grupo");
    dialog.setHeaderText(null);
    DialogPane pane = dialog.getDialogPane();
    pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

    Label body = new Label("Tem certeza que deseja excluir " + groupLabel + "?");
    body.getStyleClass().add("dialog-text");
    body.setWrapText(true);

    VBox content =
        new VBox(
            header(Icons.trash(), "Excluir grupo", "Esta ação não pode ser desfeita", true), body);
    content.getStyleClass().add("dialog-body");
    pane.setContent(content);

    Button ok = (Button) pane.lookupButton(ButtonType.OK);
    ok.setText("Excluir");
    ok.getStyleClass().add("danger");
    ok.setGraphic(Icons.trash());
    // .button:default sobrescreveria .button.danger (acento sobre perigo); além disso,
    // tirar o foco padrão do botão destrutivo evita que um Enter acidental exclua o grupo.
    ok.setDefaultButton(false);

    Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
    cancel.setText("Cancelar");
    cancel.setDefaultButton(true);

    pane.getStylesheets().add(stylesheet());

    return dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
  }

  private static Optional<String> askText(
      String title,
      FontIcon badgeIcon,
      String headerTitle,
      String subtitle,
      String label,
      String hint,
      String okText,
      FontIcon okGraphic) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle(title);
    dialog.setHeaderText(null);
    DialogPane pane = dialog.getDialogPane();
    pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

    TextField field = new TextField();
    VBox content =
        new VBox(header(badgeIcon, headerTitle, subtitle, false), fieldGroup(label, field, hint));
    content.getStyleClass().add("dialog-body");
    pane.setContent(content);

    Button ok = primaryButton(pane, okText, okGraphic);
    ok.disableProperty().bind(field.textProperty().isEmpty());

    applyTheme(pane);
    Platform.runLater(field::requestFocus);

    dialog.setResultConverter(button -> button == ButtonType.OK ? field.getText().trim() : null);
    return dialog.showAndWait().filter(value -> !value.isEmpty());
  }

  private static HBox header(FontIcon icon, String title, String subtitle, boolean danger) {
    StackPane badge = new StackPane(icon);
    badge.getStyleClass().add("dialog-icon-badge");
    if (danger) {
      badge.getStyleClass().add("danger");
    }

    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("dialog-title");
    Label subtitleLabel = new Label(subtitle);
    subtitleLabel.getStyleClass().add("dialog-subtitle");
    subtitleLabel.setWrapText(true);

    HBox row = new HBox(12, badge, new VBox(2, titleLabel, subtitleLabel));
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  private static VBox fieldGroup(String labelText, TextField field, String hintText) {
    Label label = new Label(labelText);
    label.getStyleClass().add("field-label");
    Label hint = new Label(hintText);
    hint.getStyleClass().add("field-hint");
    hint.setWrapText(true);

    VBox group = new VBox(label, field, hint);
    group.getStyleClass().add("dialog-fieldgroup");
    return group;
  }

  private static Button primaryButton(DialogPane pane, String text, FontIcon graphic) {
    Button ok = (Button) pane.lookupButton(ButtonType.OK);
    ok.setText(text);
    ok.getStyleClass().add("primary");
    if (graphic != null) {
      ok.setGraphic(graphic);
    }
    return ok;
  }

  private static void applyTheme(DialogPane pane) {
    pane.getStylesheets().add(stylesheet());
    Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
    if (cancel != null) {
      cancel.setText("Cancelar");
    }
  }

  private static String stylesheet() {
    return GroupDialogs.class.getResource("/chat-client.css").toExternalForm();
  }

  record GroupInput(String groupCode, String displayName) {}
}
