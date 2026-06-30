package br.edu.ifal.lsor.chat.gui;

import br.edu.ifal.lsor.chat.client.ChatMessage;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

final class MessageCell extends ListCell<ChatMessage> {

  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

  @Override
  protected void updateItem(ChatMessage item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
      setText(null);
      setGraphic(null);
      return;
    }
    String time = item.createdAt() != null ? TIME_FORMATTER.format(item.createdAt()) : "--:--";
    Label author = new Label("[" + time + "] " + item.authorUsername());
    author.getStyleClass().add("message-author");
    Label text = new Label(item.text());
    text.setWrapText(true);
    VBox box = new VBox(4, author, text);
    box.getStyleClass().add(item.ownMessage() ? "message-own" : "message");
    Region spacer = new Region();
    HBox row = new HBox(spacer, box);
    HBox.setHgrow(spacer, Priority.ALWAYS);
    if (!item.ownMessage()) {
      row.getChildren().setAll(box, spacer);
    }
    setGraphic(row);
  }
}
