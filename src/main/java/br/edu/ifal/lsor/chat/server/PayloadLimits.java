package br.edu.ifal.lsor.chat.server;

final class PayloadLimits {

  static final int MAX_USERNAME_LENGTH = 32;
  static final int MAX_DISPLAY_NAME_LENGTH = 80;
  static final int MAX_GROUP_CODE_LENGTH = 32;
  static final int MAX_MESSAGE_TEXT_LENGTH = 2_000;
  static final int MAX_HISTORY_MESSAGES = 1_000;

  private PayloadLimits() {}
}
