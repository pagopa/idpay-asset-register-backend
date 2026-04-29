package it.gov.pagopa.register.mapper;

import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.InitiativeSummaryDTO;
import it.gov.pagopa.register.mapper.operation.InitiativeMapper;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InitiativeMapperTest {

  private final InitiativeMapper mapper = new InitiativeMapper();


  @Test
  void toDTO_ProducersInitiative() {
    ProducersInitiative entity = ProducersInitiative.builder()
      .producersId("123")
      .initiativeId("111")
      .initiativeName("Iniziativa A")
      .initiativeStatus(InitiativeStatus.APPROVED)
      .initiativeServiceId("service1")
      .initiativeOrganizationName("MIMIT")
      .enabled(true)
      .build();

    InitiativeDTO dto = mapper.toDTO(entity);

    assertNotNull(dto);
    assertEquals("111", dto.getInitiativeId());
    assertEquals("Iniziativa A", dto.getInitiativeName());
    assertEquals("APPROVED", dto.getStatus().toString());
    assertEquals("service1", dto.getServiceId());
    assertEquals("MIMIT", dto.getOrganizationName());
    assertTrue(dto.getEnabled());
  }


  @Test
  void toDTO_InitiativeSummaryDTO() {
    InitiativeSummaryDTO entity = InitiativeSummaryDTO.builder()
      .initiativeId("111")
      .initiativeName("Iniziativa A")
      .status(InitiativeStatus.APPROVED.toString())
      .build();

    InitiativeDTO dto = mapper.toDTO(entity);

    assertNotNull(dto);
    assertEquals("111", dto.getInitiativeId());
    assertEquals("Iniziativa A", dto.getInitiativeName());
    assertEquals(InitiativeStatus.APPROVED, dto.getStatus());
    assertTrue(dto.getEnabled());
  }
}
