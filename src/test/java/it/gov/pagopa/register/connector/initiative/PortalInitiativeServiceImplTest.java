package it.gov.pagopa.register.connector.initiative;

import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeSummaryDTO;
import it.gov.pagopa.register.mapper.operation.InitiativeMapper;
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

  @Mock
  private InitiativeMapper mapper;

  @InjectMocks
  private PortalInitiativeServiceImpl service;

  @Test
  void getInitiatives_Success() {

    // given
    String organizationId = "org-123";

    InitiativeSummaryDTO summaryDTO = InitiativeSummaryDTO.builder()
        .initiativeId("111")
        .initiativeName("Iniziativa A")
        .status("ACTIVE")
        .creationDate(LocalDateTime.now())
        .build();

    InitiativeDTO initiativeDTO = InitiativeDTO.builder()
        .initiativeId("111")
        .initiativeName("Iniziativa A")
        .build();

    when(portalInitiativeRestClient.getInitiativeSummary(organizationId, null))
        .thenReturn(ResponseEntity.ok(List.of(summaryDTO)));

    when(mapper.toDTO(summaryDTO))
        .thenReturn(initiativeDTO);

    // when
    List<InitiativeDTO> result = service.getInitiatives(organizationId);

    // then
    assertEquals(1, result.size());
    assertEquals("111", result.get(0).getInitiativeId());
    assertEquals("Iniziativa A", result.get(0).getInitiativeName());

    verify(portalInitiativeRestClient)
        .getInitiativeSummary(organizationId, null);
    verify(mapper).toDTO(summaryDTO);
    verifyNoMoreInteractions(portalInitiativeRestClient, mapper);
  }
}
