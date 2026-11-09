package it.gov.pagopa.register.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum IpConnection {
  LAN("LAN"),
  WI_FI("WIFI"),
  LAN_WI_FI("LAN e WIFI"),
  NONE("Nessuna");

  private final String connection;

  IpConnection(String connection) {
    this.connection = connection;
  }

  @JsonValue
  public String getConnection() {
    return connection;
  }

  @JsonCreator
  public static IpConnection fromValue(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return Arrays.stream(values())
      .filter(connection -> connection.connection.equalsIgnoreCase(value.trim())
        || connection.name().equalsIgnoreCase(value.trim()))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported IP connection: " + value));
  }
}
