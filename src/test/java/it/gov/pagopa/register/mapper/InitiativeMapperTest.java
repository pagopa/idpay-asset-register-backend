package it.gov.pagopa.register.mapper;

import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.InitiativeSummaryDTO;
import it.gov.pagopa.register.mapper.operation.InitiativeMapper;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InitiativeMapperTest {


  @Test
  void toDTO_ProducersInitiative() {
    ProducersInitiative entity = ProducersInitiative.builder()
      .producerId("123")
      .initiativeId("111")
      .initiativeName("Iniziativa A")
      .producerEmail("email@test.it")
      .initiativeStatus(InitiativeStatus.APPROVED)
      .initiativeServiceId("service1")
      .initiativeOrganizationName("MIMIT")
      .enabled(true)
      .initiativeStartDate(LocalDateTime.now())
      .initiativeEndDate(LocalDateTime.now())
      .build();

    InitiativeDTO dto = InitiativeMapper.toDTO(entity);

    assertNotNull(dto);
    assertEquals("111", dto.getInitiativeId());
    assertEquals("Iniziativa A", dto.getInitiativeName());
    assertEquals("APPROVED", dto.getStatus().toString());
    assertEquals("service1", dto.getServiceId());
    assertEquals("MIMIT", dto.getOrganizationName());
    assertEquals("email@test.it",dto.getOrganizationEmail());
    assertTrue(dto.getEnabled());
  }


  @Test
  void toDTO_InitiativeSummaryDTO() {
    InitiativeSummaryDTO entity = InitiativeSummaryDTO.builder()
      .initiativeId("111")
      .initiativeName("Iniziativa A")
      .status(InitiativeStatus.APPROVED.toString())
      .creationDate(LocalDateTime.now())
      .updateDate(LocalDateTime.now())
      .build();

    InitiativeDTO dto = InitiativeMapper.toDTO(entity);

    assertNotNull(dto);
    assertEquals("111", dto.getInitiativeId());
    assertEquals("Iniziativa A", dto.getInitiativeName());
    assertEquals(InitiativeStatus.APPROVED, dto.getStatus());
  }
}
