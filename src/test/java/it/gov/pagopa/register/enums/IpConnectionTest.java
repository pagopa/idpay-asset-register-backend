package it.gov.pagopa.register.enums;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpConnectionTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serializesUsingExternalValue() {
    assertEquals("\"LAN e WI-FI\"", objectMapper.writeValueAsString(IpConnection.LAN_WI_FI));
  }

  @Test
  void deserializesExternalValue() {
    assertEquals(IpConnection.WI_FI, objectMapper.readValue("\"WI-FI\"", IpConnection.class));
  }
}
