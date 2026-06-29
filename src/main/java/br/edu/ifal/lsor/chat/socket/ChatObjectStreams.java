package br.edu.ifal.lsor.chat.socket;

import java.io.IOException;
import java.io.ObjectOutputStream;

public final class ChatObjectStreams {

  private ChatObjectStreams() {}

  public static void writeAndReset(ObjectOutputStream output, Object object) throws IOException {
    output.writeObject(object);
    output.reset();
    output.flush();
  }
}
