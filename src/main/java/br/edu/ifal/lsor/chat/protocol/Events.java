package br.edu.ifal.lsor.chat.protocol;

public final class Events {

  public static final String USER_ONLINE = "USER_ONLINE";
  public static final String USER_OFFLINE = "USER_OFFLINE";
  public static final String DIRECT_MESSAGE = "DIRECT_MESSAGE";
  public static final String GROUP_MESSAGE = "GROUP_MESSAGE";
  public static final String GROUP_CREATED = "GROUP_CREATED";
  public static final String GROUP_RENAMED = "GROUP_RENAMED";
  public static final String GROUP_DELETED = "GROUP_DELETED";
  public static final String GROUP_MEMBER_JOINED = "GROUP_MEMBER_JOINED";
  public static final String GROUP_MEMBER_LEFT = "GROUP_MEMBER_LEFT";

  private Events() {}
}
