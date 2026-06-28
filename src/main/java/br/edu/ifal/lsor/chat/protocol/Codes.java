package br.edu.ifal.lsor.chat.protocol;

public final class Codes {

  public static final String LOGIN_ACCEPTED = "LOGIN_ACCEPTED";
  public static final String LOGOUT_ACCEPTED = "LOGOUT_ACCEPTED";
  public static final String HEARTBEAT_ACK = "HEARTBEAT_ACK";
  public static final String USERS_LISTED = "USERS_LISTED";
  public static final String GROUPS_LISTED = "GROUPS_LISTED";
  public static final String GROUP_CREATED = "GROUP_CREATED";
  public static final String GROUP_JOINED = "GROUP_JOINED";
  public static final String GROUP_LEFT = "GROUP_LEFT";
  public static final String GROUP_RENAMED = "GROUP_RENAMED";
  public static final String GROUP_DELETED = "GROUP_DELETED";
  public static final String MESSAGE_ACCEPTED = "MESSAGE_ACCEPTED";
  public static final String HISTORY_RETURNED = "HISTORY_RETURNED";
  public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
  public static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";
  public static final String UNKNOWN_ACTION = "UNKNOWN_ACTION";
  public static final String USERNAME_ALREADY_ONLINE = "USERNAME_ALREADY_ONLINE";
  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
  public static final String USER_OFFLINE = "USER_OFFLINE";
  public static final String CANNOT_MESSAGE_SELF = "CANNOT_MESSAGE_SELF";
  public static final String GROUP_ALREADY_EXISTS = "GROUP_ALREADY_EXISTS";
  public static final String GROUP_NOT_FOUND = "GROUP_NOT_FOUND";
  public static final String ALREADY_GROUP_MEMBER = "ALREADY_GROUP_MEMBER";
  public static final String NOT_GROUP_MEMBER = "NOT_GROUP_MEMBER";
  public static final String OWNER_CANNOT_LEAVE = "OWNER_CANNOT_LEAVE";
  public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
  public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

  private Codes() {}
}
