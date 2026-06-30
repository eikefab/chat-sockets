package br.edu.ifal.lsor.chat.gui;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

final class Icons {

  private Icons() {}

  static FontIcon chat() {
    return new FontIcon(MaterialDesignC.CHAT_OUTLINE);
  }

  static FontIcon login() {
    return new FontIcon(MaterialDesignL.LOGIN);
  }

  static FontIcon send() {
    return new FontIcon(MaterialDesignS.SEND);
  }

  static FontIcon plus() {
    return new FontIcon(MaterialDesignP.PLUS);
  }

  static FontIcon importIcon() {
    return new FontIcon(MaterialDesignI.IMPORT);
  }

  static FontIcon logout() {
    return new FontIcon(MaterialDesignL.LOGOUT);
  }

  static FontIcon trash() {
    return new FontIcon(MaterialDesignT.TRASH_CAN_OUTLINE);
  }

  static FontIcon accountGroup() {
    return new FontIcon(MaterialDesignA.ACCOUNT_GROUP_OUTLINE);
  }
}
