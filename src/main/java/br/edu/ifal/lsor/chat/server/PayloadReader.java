package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import java.io.Serializable;

final class PayloadReader {

  private final ClientRequest request;

  private PayloadReader(ClientRequest request) {
    this.request = request;
  }

  static PayloadReader from(ClientRequest request) {
    return new PayloadReader(request);
  }

  String requiredString(String key) throws InvalidPayloadException {
    Serializable value = request.payload().get(key);
    if (!(value instanceof String text)) {
      throw new InvalidPayloadException("Informe " + key + ".");
    }
    String trimmed = text.trim();
    if (trimmed.isEmpty()) {
      throw new InvalidPayloadException("Informe " + key + ".");
    }
    return trimmed;
  }

  boolean optionalBoolean(String key, boolean defaultValue) throws InvalidPayloadException {
    Serializable value = request.payload().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (!(value instanceof Boolean booleanValue)) {
      throw new InvalidPayloadException("Informe " + key + " válido.");
    }
    return booleanValue;
  }

  int optionalLimit(String key, int defaultValue, int maxValue) throws InvalidPayloadException {
    Serializable value = request.payload().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (!(value instanceof Integer limit)) {
      throw new InvalidPayloadException("Informe " + key + " válido.");
    }
    if (limit < 1) {
      throw new InvalidPayloadException("Informe " + key + " válido.");
    }
    return Math.min(limit, maxValue);
  }

  ActionPayloads.LoginPayload login() throws InvalidPayloadException {
    return new ActionPayloads.LoginPayload(
        requiredString("username"), requiredString("displayName"));
  }

  ActionPayloads.GroupCodePayload groupCode() throws InvalidPayloadException {
    return new ActionPayloads.GroupCodePayload(requiredString("groupCode"));
  }

  ActionPayloads.GroupDisplayPayload groupDisplay() throws InvalidPayloadException {
    return new ActionPayloads.GroupDisplayPayload(
        requiredString("groupCode"), requiredString("displayName"));
  }

  ActionPayloads.SendDirectPayload sendDirect() throws InvalidPayloadException {
    return new ActionPayloads.SendDirectPayload(
        requiredString("targetUsername"), requiredString("text"));
  }

  ActionPayloads.SendGroupPayload sendGroup() throws InvalidPayloadException {
    return new ActionPayloads.SendGroupPayload(requiredString("groupCode"), requiredString("text"));
  }

  ActionPayloads.HistoryPayload history(int maxLimit) throws InvalidPayloadException {
    return new ActionPayloads.HistoryPayload(
        requiredString("scope"), requiredString("target"), optionalLimit("limit", 50, maxLimit));
  }

  ActionPayloads.ListGroupsPayload listGroups() throws InvalidPayloadException {
    return new ActionPayloads.ListGroupsPayload(optionalBoolean("onlyMine", false));
  }
}
