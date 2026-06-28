package br.edu.ifal.lsor.chat.socket;

import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

public final class ChatObjectInputFilters {

  private static final String PROTOCOL_FILTER_PATTERN =
      "br.edu.ifal.lsor.chat.protocol.*;"
          + "java.lang.*;"
          + "java.util.*;"
          + "java.time.*;"
          + "maxdepth=20;maxrefs=1000;maxbytes=1000000;maxarray=1000000;"
          + "!*";

  private ChatObjectInputFilters() {}

  public static void applyProtocolFilter(ObjectInputStream input) {
    input.setObjectInputFilter(ObjectInputFilter.Config.createFilter(PROTOCOL_FILTER_PATTERN));
  }
}
