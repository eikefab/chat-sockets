package br.edu.ifal.lsor.chat.server;

final class ActionPayloads {

  private ActionPayloads() {}

  record LoginPayload(String username, String displayName) {}

  record GroupCodePayload(String groupCode) {}

  record GroupDisplayPayload(String groupCode, String displayName) {}

  record SendDirectPayload(String targetUsername, String text) {}

  record SendGroupPayload(String groupCode, String text) {}

  record HistoryPayload(MessageScope scope, String target, int limit) {}

  record ListGroupsPayload(boolean onlyMine) {}
}
