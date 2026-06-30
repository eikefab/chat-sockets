package br.edu.ifal.lsor.chat.socket;

import java.io.ObjectInputFilter;

public final class ChatObjectInputFilters {

  private static final String PROTOCOL_FILTER_PATTERN =
      "maxdepth=20;maxarray=1000000;"
          + "br.edu.ifal.lsor.chat.protocol.ClientRequest;"
          + "br.edu.ifal.lsor.chat.protocol.ServerResponse;"
          + "br.edu.ifal.lsor.chat.protocol.ServerEvent;"
          + "java.lang.Object;"
          + "java.lang.Record;"
          + "java.lang.Comparable;"
          + "java.lang.Number;"
          + "java.lang.String;"
          + "java.lang.Boolean;"
          + "java.lang.Integer;"
          + "java.lang.Long;"
          + "java.util.UUID;"
          + "java.util.ArrayList;"
          + "java.util.HashMap;"
          + "java.util.LinkedHashMap;"
          + "java.util.ImmutableCollections$*;"
          + "java.util.CollSer;"
          + "java.time.Instant;"
          + "java.time.Ser;"
          + "!*";

  private static final ObjectInputFilter PROTOCOL_FILTER =
      ObjectInputFilter.Config.createFilter(PROTOCOL_FILTER_PATTERN);

  private ChatObjectInputFilters() {}

  public static ObjectInputFilter protocolFilter() {
    return PROTOCOL_FILTER;
  }
}
