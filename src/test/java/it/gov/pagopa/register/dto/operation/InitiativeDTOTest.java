package it.gov.pagopa.register.dto.operation;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InitiativeDTOTest {

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @Test
  void shouldDeserializePortalInitiativeDetailWithNestedFieldsAndIgnoreUnknownProperties() throws Exception {
    String json = """
      {
        "initiativeId": "111",
        "initiativeName": "Iniziativa 1",
        "status": "PUBLISHED",
        "organizationId": "producer-123",
        "organizationName": "MIMIT",
        "creationDate": "2026-01-01T10:00:00",
        "general": {
          "startDate": "2025-12-31",
          "endDate": "2026-12-30",
          "rankingEnabled": false
        },
        "additionalInfo": {
          "serviceId": "1234567890",
          "logoFileName": "logo.png"
        },
        "beneficiaryRule": {
          "selfDeclarationCriteria": []
        }
      }
      """;

    InitiativeDTO result = objectMapper.readValue(json, InitiativeDTO.class);

    assertEquals("111", result.getInitiativeId());
    assertEquals("Iniziativa 1", result.getInitiativeName());
    assertEquals(InitiativeStatus.PUBLISHED, result.getStatus());
    assertEquals("producer-123", result.getOrganizationId());
    assertEquals("MIMIT", result.getOrganizationName());
    assertEquals(LocalDate.of(2025, 12, 31), result.getStartDate());
    assertEquals(LocalDate.of(2026, 12, 30), result.getEndDate());
    assertEquals("1234567890", result.getServiceId());
  }

  @Test
  void directFieldsShouldTakePrecedenceOverNestedFields() {
    InitiativeDTO dto = InitiativeDTO.builder()
      .startDate(LocalDate.of(2026, 1, 1))
      .endDate(LocalDate.of(2026, 12, 31))
      .serviceId("direct-service")
      .general(new InitiativeDTO.InitiativeGeneralDTO(
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 12, 31)
      ))
      .additionalInfo(new InitiativeDTO.InitiativeAdditionalDTO("nested-service"))
      .build();

    assertEquals(LocalDate.of(2026, 1, 1), dto.getStartDate());
    assertEquals(LocalDate.of(2026, 12, 31), dto.getEndDate());
    assertEquals("direct-service", dto.getServiceId());
  }
}
