package br.edu.ifal.lsor.chat.gui;

import br.edu.ifal.lsor.chat.client.ConversationKind;
import br.edu.ifal.lsor.chat.client.ConversationTarget;
import javafx.scene.control.ListCell;

final class ConversationCell extends ListCell<ConversationTarget> {

  @Override
  protected void updateItem(ConversationTarget item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
      setText(null);
      return;
    }
    setText((item.kind() == ConversationKind.GROUP ? "# " : "@ ") + item.label());
  }
}
