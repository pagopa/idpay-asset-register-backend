package it.gov.pagopa.register.connector.initiative;

import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.InitiativeSummaryDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalInitiativeServiceImplTest {

  @Mock
  private PortalInitiativeRestClient portalInitiativeRestClient;

  @InjectMocks
  private PortalInitiativeServiceImpl service;

  @Test
  void getInitiatives_Success() {

    // given
    String organizationId = "org-123";

    InitiativeSummaryDTO summaryDTO = InitiativeSummaryDTO.builder()
      .initiativeId("111")
      .initiativeName("Iniziativa A")
      .status("PUBLISHED")
      .creationDate(LocalDateTime.of(2024, 1, 10, 10, 0))
      .build();

    when(portalInitiativeRestClient.getInitiativeSummary(organizationId, null))
      .thenReturn(ResponseEntity.ok(List.of(summaryDTO)));

    // when
    List<InitiativeDTO> result = service.getInitiatives(organizationId);

    // then
    assertEquals(1, result.size());

    InitiativeDTO dto = result.get(0);
    assertEquals("111", dto.getInitiativeId());
    assertEquals("Iniziativa A", dto.getInitiativeName());
    assertEquals(InitiativeStatus.PUBLISHED, dto.getStatus());
    assertEquals(
      summaryDTO.getStartDate(),
      dto.getStartDate()
    );

    verify(portalInitiativeRestClient)
      .getInitiativeSummary(organizationId, null);
    verifyNoMoreInteractions(portalInitiativeRestClient);
  }
}
