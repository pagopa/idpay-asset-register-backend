package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.initiative.PortalInitiativeService;
import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitiativeServiceTest {

  @Mock
  private ProducersInitiativeRepository repository;

  @Mock
  private PortalInitiativeService portalInitiativeService;

  @InjectMocks
  private InitiativeService service;

  @Test
  void getInitiatives_WhenRoleIsOperatore_ReturnsEnabledInitiativesOrderedByName() {

    // given
    String organizationId = "org-123";
    String role = UserRole.OPERATORE.getRole();
    LocalDateTime today = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
    LocalDateTime fixedStartDate = today.minusDays(7);
    LocalDateTime fixedEndDate = today.plusDays(30);

    ProducersInitiative firstInitiative = ProducersInitiative.builder()
      .initiativeId("111")
      .initiativeName("Alpha initiative")
      .enabled(true)
      .initiativeStatus(InitiativeStatus.PUBLISHED)
      .initiativeEndDate(fixedEndDate)
      .initiativeStartDate(fixedStartDate)
      .build();

    ProducersInitiative disabledInitiative = ProducersInitiative.builder()
      .initiativeId("222")
      .initiativeName("Disabled initiative")
      .enabled(false)
      .initiativeStatus(InitiativeStatus.PUBLISHED)
      .initiativeEndDate(fixedEndDate)
      .initiativeStartDate(fixedStartDate)
      .build();

    ProducersInitiative secondInitiative = ProducersInitiative.builder()
      .initiativeId("333")
      .initiativeName("Beta initiative")
      .enabled(true)
      .initiativeStatus(InitiativeStatus.PUBLISHED)
      .initiativeEndDate(fixedEndDate)
      .initiativeStartDate(fixedStartDate)
      .build();

    when(repository.findByProducerIdOrderByInitiativeNameAsc(organizationId))
      .thenReturn(List.of(firstInitiative, secondInitiative, disabledInitiative));

    // when
    List<InitiativeDTO> result =
      service.getInitiatives(role, organizationId);

    // then
    assertEquals(2, result.size());

    InitiativeDTO firstDto = result.getFirst();
    assertEquals("111", firstDto.getInitiativeId());
    assertEquals("Alpha initiative", firstDto.getInitiativeName());
    assertTrue(firstDto.getEnabled());

    InitiativeDTO secondDto = result.get(1);
    assertEquals("333", secondDto.getInitiativeId());
    assertEquals("Beta initiative", secondDto.getInitiativeName());
    assertTrue(secondDto.getEnabled());

    verify(repository).findByProducerIdOrderByInitiativeNameAsc(organizationId);
    verifyNoInteractions(portalInitiativeService);
  }

  @Test
  void getInitiatives_WhenRoleIsNotOperatore_UsesPortalInitiativeService() {

    // given
    String organizationId = "org-123";
    String role = UserRole.INVITALIA.getRole();

    InitiativeDTO dto = InitiativeDTO.builder()
      .initiativeId("999")
      .build();

    when(portalInitiativeService.getInitiatives(organizationId))
      .thenReturn(List.of(dto));

    // when
    List<InitiativeDTO> result =
      service.getInitiatives(role, organizationId);

    // then
    assertEquals(1, result.size());
    assertEquals("999", result.getFirst().getInitiativeId());

    verify(portalInitiativeService).getInitiatives(organizationId);
    verifyNoInteractions(repository);
  }

  @Test
  void getInitiatives_WhenRoleIsSupport_ReturnsEnabledInitiativesOrderedByName() {

    // given
    String organizationId = "org-123";
    String role = UserRole.SUPPORT.getRole();
    LocalDateTime today = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
    LocalDateTime fixedStartDate = today.minusDays(7);
    LocalDateTime fixedEndDate = today.plusDays(30);

    ProducersInitiative initiative = ProducersInitiative.builder()
      .initiativeId("555")
      .initiativeName("Support initiative")
      .enabled(true)
      .initiativeStatus(InitiativeStatus.PUBLISHED)
      .initiativeEndDate(fixedEndDate)
      .initiativeStartDate(fixedStartDate)
      .build();

    when(repository.findByProducerIdOrderByInitiativeNameAsc(organizationId))
      .thenReturn(List.of(initiative));

    // when
    List<InitiativeDTO> result = service.getInitiatives(role, organizationId);

    // then
    assertEquals(1, result.size());
    assertEquals("555", result.getFirst().getInitiativeId());
    assertTrue(result.getFirst().getEnabled());

    verify(repository).findByProducerIdOrderByInitiativeNameAsc(organizationId);
    verifyNoInteractions(portalInitiativeService);
  }
}
