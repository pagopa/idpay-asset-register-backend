package it.gov.pagopa.register.controller.role;

import it.gov.pagopa.register.controller.role.consent.PortalConsentControllerImpl;
import it.gov.pagopa.register.dto.role.PortalConsentDTO;
import it.gov.pagopa.register.service.role.PortalConsentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value={PortalConsentControllerImpl.class}, excludeAutoConfiguration =  { UserDetailsServiceAutoConfiguration.class , SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class PortalConsentControllerTest {

    //region String constants
    private static final String BASE_URL = "/idpay/consent";
    private static final String UID_PARAM_NAME = "userId";
    private static final String USER_ID = "195da70f-d3f0-4c57-b62e-ef471348e920";
    private static final String VERSION_ID = "VERSION_ID";
    //endregion

    private static final PortalConsentDTO EMPTY_CONSENT_DTO = new PortalConsentDTO();

    @MockitoBean
    private PortalConsentService service;
    @Autowired
    private MockMvc mvc;

    @Test
    void testGetSuccess() throws Exception {
        Mockito.when(service.get(USER_ID)).thenReturn(EMPTY_CONSENT_DTO);

        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .param(UID_PARAM_NAME, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().string("{}"))
                .andReturn();
    }

    @Test
    void testGetOkFirstAcceptance() throws Exception {
        PortalConsentDTO consent = new PortalConsentDTO(VERSION_ID, true);
        Mockito.when(service.get(USER_ID)).thenReturn(consent);

        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .param(UID_PARAM_NAME, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        Assertions.assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void testGetOkNewVersion() throws Exception {
        PortalConsentDTO consent = new PortalConsentDTO(VERSION_ID, false);
        Mockito.when(service.get(USER_ID)).thenReturn(consent);

        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .param(UID_PARAM_NAME, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        Assertions.assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void testSaveOk() throws Exception {
        String consentString = "{\"versionId\":\"%s\"}".formatted(VERSION_ID);

        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL)
                        .param(UID_PARAM_NAME, USER_ID)
                        .content(consentString)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

  @Test
  void testRemoveConsentOk() throws Exception {
    mvc.perform(MockMvcRequestBuilders
        .delete(BASE_URL)
        .param(UID_PARAM_NAME, USER_ID)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andReturn();
  }

}
