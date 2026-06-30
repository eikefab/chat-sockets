package br.edu.ifal.lsor.chat.gui;

import br.edu.ifal.lsor.chat.client.ConversationKind;
import br.edu.ifal.lsor.chat.client.ConversationTarget;
import java.util.function.Predicate;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;

final class ConversationCell extends ListCell<ConversationTarget> {

  private final FontIcon icon = new FontIcon();
  private final Circle statusDot = new Circle(4);
  private final Label label = new Label();
  private final Label meta = new Label();
  private final HBox row;
  private final Predicate<String> isOnline;

  ConversationCell(Predicate<String> isOnline) {
    this.isOnline = isOnline;
    icon.setId("conv-icon");
    label.setId("conv-label");
    meta.setId("conv-meta");
    statusDot.setId("status-dot");
    row = new HBox(10, icon, new VBox(2, label, meta), statusDot);
    row.setId("conv-row");
    HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
    setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
    setGraphic(row);
  }

  @Override
  protected void updateItem(ConversationTarget item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
      setGraphic(null);
      return;
    }
    setGraphic(row);
    boolean group = item.kind() == ConversationKind.GROUP;
    icon.setIconCode(
        group ? MaterialDesignA.ACCOUNT_GROUP_OUTLINE : MaterialDesignA.ACCOUNT_OUTLINE);
    label.setText(item.label());
    if (group) {
      meta.setText("Grupo");
      statusDot.setVisible(false);
    } else {
      boolean online = isOnline.test(item.id());
      meta.setText(online ? "Online" : "Offline");
      statusDot.setVisible(true);
      statusDot
          .getStyleClass()
          .setAll("status-dot", online ? "status-dot-online" : "status-dot-offline");
    }
  }
}
