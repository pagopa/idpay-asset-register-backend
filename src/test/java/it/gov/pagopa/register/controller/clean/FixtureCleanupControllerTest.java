package it.gov.pagopa.register.controller.clean;

import it.gov.pagopa.common.web.exception.ErrorManager;
import it.gov.pagopa.register.exception.role.ConsentNotFoundException;
import it.gov.pagopa.register.service.clean.FixtureCleanupService;
import it.gov.pagopa.register.service.role.PortalConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FixtureCleanupControllerTest {
  private static final String BASE = "/idpay/register/clean/fixtures";
  private static final String VALID_FILE = """
      {"organizationId":"83843864-f3c0-4def-badb-7f197471b72e",
       "initiativeId":"65c3b1e3e4b0a1a2b3c4d5e6","fileName":"unique.csv"}
      """;
  private FixtureCleanupService service;
  private PortalConsentService consents;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    service = mock(FixtureCleanupService.class);
    consents = mock(PortalConsentService.class);
    mvc = MockMvcBuilders.standaloneSetup(new FixtureCleanupController(service, consents))
        .setControllerAdvice(new ErrorManager(null)).build();
  }

  @Test
  void capabilitiesAreAvailableWithoutDedicatedCredential() throws Exception {
    mvc.perform(get(BASE + "/capabilities")).andExpect(status().isOk());
  }

  @Test
  void activeUploadReturnsHttp409() throws Exception {
    when(service.deleteFiles(any())).thenReturn(false);
    mvc.perform(post(BASE + "/files/delete")
        .contentType(MediaType.APPLICATION_JSON).content(VALID_FILE))
        .andExpect(status().isConflict());
  }

  @Test
  void successfulScopedDeletionReturnsHttp204() throws Exception {
    when(service.deleteFiles(any())).thenReturn(true);
    mvc.perform(post(BASE + "/files/delete")
        .contentType(MediaType.APPLICATION_JSON).content(VALID_FILE))
        .andExpect(status().isNoContent());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"organizationId\":\"invalid\",\"initiativeId\":\"invalid\",\"fileName\":\"\"}"})
  void invalidScopeDoesNotReachService(String payload) throws Exception {
    mvc.perform(post(BASE + "/files/delete")
        .contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service, consents);
  }

  @Test
  void consentDeletionIsScopedToProvidedTestUser() throws Exception {
    mvc.perform(post(BASE + "/consents/delete")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"userId\":\"83843864-f3c0-4def-badb-7f197471b72e\"}"))
        .andExpect(status().isNoContent());
    verify(consents).remove("83843864-f3c0-4def-badb-7f197471b72e");
  }

  @Test
  void absentConsentIsAlreadyClean() throws Exception {
    doThrow(new ConsentNotFoundException("No consent")).when(consents)
        .remove("83843864-f3c0-4def-badb-7f197471b72e");
    mvc.perform(post(BASE + "/consents/delete")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"userId\":\"83843864-f3c0-4def-badb-7f197471b72e\"}"))
        .andExpect(status().isNoContent());
  }

}
