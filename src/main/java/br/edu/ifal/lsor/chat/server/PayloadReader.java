package br.edu.ifal.lsor.chat.server;

import br.edu.ifal.lsor.chat.protocol.ClientRequest;
import java.io.Serializable;
import java.util.Locale;

final class PayloadReader {

  private final ClientRequest request;

  private PayloadReader(ClientRequest request) {
    this.request = request;
  }

  static PayloadReader from(ClientRequest request) {
    return new PayloadReader(request);
  }

  String requiredString(String key, int maxLength) throws InvalidPayloadException {
    Serializable value = request.payload().get(key);
    if (!(value instanceof String text)) {
      throw new InvalidPayloadException("Informe " + key + ".");
    }
    String trimmed = text.trim();
    if (trimmed.isEmpty()) {
      throw new InvalidPayloadException("Informe " + key + ".");
    }
    if (trimmed.length() > maxLength) {
      throw new InvalidPayloadException(
          "Informe " + key + " com ate " + maxLength + " caracteres.");
    }
    return trimmed;
  }

  String requiredUsername(String key) throws InvalidPayloadException {
    return requiredString(key, PayloadLimits.MAX_USERNAME_LENGTH).toLowerCase(Locale.ROOT);
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
        requiredUsername("username"),
        requiredString("displayName", PayloadLimits.MAX_DISPLAY_NAME_LENGTH));
  }

  ActionPayloads.GroupCodePayload groupCode() throws InvalidPayloadException {
    return new ActionPayloads.GroupCodePayload(
        requiredString("groupCode", PayloadLimits.MAX_GROUP_CODE_LENGTH));
  }

  ActionPayloads.GroupDisplayPayload groupDisplay() throws InvalidPayloadException {
    return new ActionPayloads.GroupDisplayPayload(
        requiredString("groupCode", PayloadLimits.MAX_GROUP_CODE_LENGTH),
        requiredString("displayName", PayloadLimits.MAX_DISPLAY_NAME_LENGTH));
  }

  ActionPayloads.SendDirectPayload sendDirect() throws InvalidPayloadException {
    return new ActionPayloads.SendDirectPayload(
        requiredUsername("targetUsername"),
        requiredString("text", PayloadLimits.MAX_MESSAGE_TEXT_LENGTH));
  }

  ActionPayloads.SendGroupPayload sendGroup() throws InvalidPayloadException {
    return new ActionPayloads.SendGroupPayload(
        requiredString("groupCode", PayloadLimits.MAX_GROUP_CODE_LENGTH),
        requiredString("text", PayloadLimits.MAX_MESSAGE_TEXT_LENGTH));
  }

  ActionPayloads.HistoryPayload history(int maxLimit) throws InvalidPayloadException {
    MessageScope scope = MessageScope.parse(requiredString("scope", 16));
    int targetLimit =
        switch (scope) {
          case DIRECT -> PayloadLimits.MAX_USERNAME_LENGTH;
          case GROUP -> PayloadLimits.MAX_GROUP_CODE_LENGTH;
        };
    return new ActionPayloads.HistoryPayload(
        scope, requiredTarget(scope, targetLimit), optionalLimit("limit", 50, maxLimit));
  }

  ActionPayloads.ListGroupsPayload listGroups() throws InvalidPayloadException {
    return new ActionPayloads.ListGroupsPayload(optionalBoolean("onlyMine", true));
  }

  private String requiredTarget(MessageScope scope, int maxLength) throws InvalidPayloadException {
    if (scope == MessageScope.DIRECT) {
      return requiredUsername("target");
    }
    return requiredString("target", maxLength);
  }
}
