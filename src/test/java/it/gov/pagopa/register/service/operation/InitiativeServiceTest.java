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
  void getInitiatives_WhenRoleIsOperatore_ReturnsOnlyEnabledInitiatives() {

    // given
    String organizationId = "org-123";
    String role = UserRole.OPERATORE.getRole();

    ProducersInitiative enabled = ProducersInitiative.builder()
      .initiativeId("111")
      .initiativeName("Test initiative")
      .enabled(true)
      .initiativeStatus(InitiativeStatus.PUBLISHED)
      .build();

    ProducersInitiative disabled = ProducersInitiative.builder()
      .initiativeId("222")
      .enabled(false)
      .build();

    when(repository.findByProducersId(organizationId))
      .thenReturn(List.of(enabled, disabled));

    // when
    List<InitiativeDTO> result =
      service.getInitiatives(role, organizationId);

    // then
    assertEquals(1, result.size());

    InitiativeDTO dto = result.get(0);
    assertEquals("111", dto.getInitiativeId());
    assertEquals("Test initiative", dto.getInitiativeName());
    assertTrue(dto.getEnabled());

    verify(repository).findByProducersId(organizationId);
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
    assertEquals("999", result.get(0).getInitiativeId());

    verify(portalInitiativeService).getInitiatives(organizationId);
    verifyNoInteractions(repository);
  }
}
