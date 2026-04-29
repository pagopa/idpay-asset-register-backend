package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.connector.initiative.PortalInitiativeRestClient;
import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.service.operation.InitiativeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
  value = InitiativeController.class,
  excludeAutoConfiguration = {
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class
  }
)
@AutoConfigureMockMvc(addFilters = false)
class InitiativeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private InitiativeService service;

  @MockitoBean
  private PortalInitiativeRestClient portalInitiativeRestClient;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void getInitiativeEnabled_Success() throws Exception {

    // given
    String role = UserRole.OPERATORE.getRole();
    String organizationId = UUID.randomUUID().toString();

    InitiativeDTO dto = InitiativeDTO.builder()
      .initiativeId("111")
      .initiativeName("Iniziativa A")
      .build();

    when(service.getInitiatives(role, organizationId))
      .thenReturn(List.of(dto));

    // when & then
    mockMvc.perform(
        post("/idpay/register/initiative")
          .header("x-organization-role", role)
          .header("x-organization-id", organizationId)
          .contentType(MediaType.APPLICATION_JSON)
      )
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].initiativeId").value("111"))
      .andExpect(jsonPath("$[0].initiativeName").value("Iniziativa A"));

    verify(service).getInitiatives(role, organizationId);
  }

  @Test
  void getInitiativeEnabled_ServiceThrowsException_InternalServerError() throws Exception {

    // given
    String role = UserRole.OPERATORE.getRole();
    String organizationId = UUID.randomUUID().toString();

    when(service.getInitiatives(role, organizationId))
      .thenThrow(new RuntimeException("Service error"));

    // when & then
    mockMvc.perform(
        post("/idpay/register/initiative")
          .header("x-organization-role", role)
          .header("x-organization-id", organizationId)
          .contentType(MediaType.APPLICATION_JSON)
      )
      .andExpect(status().isInternalServerError());

    verify(service).getInitiatives(role, organizationId);
  }
}
