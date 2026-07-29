package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

enum TestEnum {
  VALUE_ONE, VALUE_TWO
}

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@WebMvcTest(value = {ValidationExceptionHandlerTest.TestController.class}, excludeAutoConfiguration = { UserDetailsServiceAutoConfiguration.class , SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
  ValidationExceptionHandlerTest.TestController.class,
  ValidationExceptionHandler.class})
class ValidationExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockitoSpyBean
  private TestController testControllerSpy;

  @Autowired
  private ValidationExceptionHandler validationExceptionHandler;

  @RestController
  @Slf4j
  static class TestController {

    @PutMapping("/test")
    String testEndpoint(@RequestBody @Valid ValidationDTO body, @RequestHeader("data") String data) {
      return "OK";
    }

    @GetMapping("/test-enum")
    String testEnumEndpoint(@RequestParam TestEnum enumParam, @RequestParam int intParam) {
      return "OK";
    }
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class ValidationDTO {
    @NotBlank(message = "The field is mandatory!")
    private String data;
  }

  private final ValidationDTO validationDTO = new ValidationDTO("data");

  @Test
  void handleMethodArgumentNotValidException() throws Exception {

    mockMvc.perform(MockMvcRequestBuilders.put("/test")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new ValidationDTO("")))
        .header("data", "data")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(MockMvcResultMatchers.status().isBadRequest())
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("[data]: The field is mandatory!"));
  }

  @Test
  void handleMissingRequestHeaderException() throws Exception {

    mockMvc.perform(MockMvcRequestBuilders.put("/test")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validationDTO))
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(MockMvcResultMatchers.status().isBadRequest())
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Required request header 'data' for method parameter type String is not present"));

  }

  @Test
  void handleMethodArgumentTypeMismatchException_InvalidEnum() throws Exception {

    mockMvc.perform(MockMvcRequestBuilders.get("/test-enum")
        .queryParam("enumParam", "INVALID_ENUM_VALUE")
        .queryParam("intParam", "123")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(MockMvcResultMatchers.status().isBadRequest())
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("[enumParam]")))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("INVALID_ENUM_VALUE")))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("VALUE_ONE")))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("VALUE_TWO")));
  }

  @Test
  void handleMethodArgumentTypeMismatchException_InvalidInt() throws Exception {

    mockMvc.perform(MockMvcRequestBuilders.get("/test-enum")
        .queryParam("enumParam", "VALUE_ONE")
        .queryParam("intParam", "NOT_AN_INT")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(MockMvcResultMatchers.status().isBadRequest())
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("[intParam]")));
  }

  @Test
  void handleMethodArgumentTypeMismatchException_EmptyInt() throws Exception {

    mockMvc.perform(MockMvcRequestBuilders.get("/test-enum")
        .queryParam("enumParam", "VALUE_ONE")
        .queryParam("intParam", "")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(MockMvcResultMatchers.status().isBadRequest())
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message")
        .value("[intParam]: invalid request parameter"));
  }

  @Test
  void handleMethodArgumentTypeMismatchException_NullTypeAndValue() {
    MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(exception.getName()).thenReturn("unknownParam");
    when(exception.getRequiredType()).thenReturn(null);
    when(exception.getValue()).thenReturn(null);
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/test");

    ErrorDTO response =
      validationExceptionHandler.handleMethodArgumentTypeMismatchException(exception, request);

    assertEquals("INVALID_REQUEST", response.getCode());
    assertEquals("[unknownParam]: invalid request parameter", response.getMessage());
  }
}
