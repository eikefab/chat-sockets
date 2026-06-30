package br.edu.ifal.lsor.chat.gui;

import br.edu.ifal.lsor.chat.client.ChatMessage;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

final class MessageCell extends ListCell<ChatMessage> {

  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

  private final Label author = new Label();
  private final Label time = new Label();
  private final Label text = new Label();
  private final FontIcon authorIcon = new FontIcon();
  private final HBox authorRow = new HBox(6, author, time);
  private final VBox bubble = new VBox(3, authorRow, text);
  private final Region spacer = new Region();
  private final HBox row = new HBox(spacer, bubble);

  MessageCell() {
    text.setWrapText(true);
    author.setGraphic(authorIcon);
    author.getStyleClass().add("message-author");
    time.getStyleClass().add("message-time");
    text.getStyleClass().add("message-text");
    authorRow.setAlignment(Pos.BASELINE_LEFT);
    HBox.setHgrow(spacer, Priority.ALWAYS);
    setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
    setGraphic(row);
  }

  @Override
  protected void updateItem(ChatMessage item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
      setGraphic(null);
      return;
    }
    setGraphic(row);
    String when = item.createdAt() != null ? TIME_FORMATTER.format(item.createdAt()) : "--:--";
    author.setText(item.authorUsername());
    time.setText(when);
    text.setText(item.text());
    boolean own = item.ownMessage();
    authorIcon.setIconCode(own ? MaterialDesignC.CHECK_ALL : MaterialDesignA.ACCOUNT);
    bubble.getStyleClass().setAll(own ? "message-own" : "message");
    row.getStyleClass().setAll("message-row");
    if (own) {
      row.getStyleClass().add("message-row-own");
    }
    if (own) {
      row.getChildren().setAll(spacer, bubble);
    } else {
      row.getChildren().setAll(bubble, spacer);
    }
  }
}
