package br.edu.ifal.lsor.chat.protocol;

public final class Actions {

  public static final String LOGIN = "LOGIN";
  public static final String LOGOUT = "LOGOUT";
  public static final String HEARTBEAT = "HEARTBEAT";
  public static final String LIST_USERS = "LIST_USERS";
  public static final String LIST_GROUPS = "LIST_GROUPS";
  public static final String CREATE_GROUP = "CREATE_GROUP";
  public static final String JOIN_GROUP = "JOIN_GROUP";
  public static final String LEAVE_GROUP = "LEAVE_GROUP";
  public static final String RENAME_GROUP = "RENAME_GROUP";
  public static final String DELETE_GROUP = "DELETE_GROUP";
  public static final String SEND_DIRECT = "SEND_DIRECT";
  public static final String SEND_GROUP = "SEND_GROUP";
  public static final String GET_HISTORY = "GET_HISTORY";

  private Actions() {}
}
