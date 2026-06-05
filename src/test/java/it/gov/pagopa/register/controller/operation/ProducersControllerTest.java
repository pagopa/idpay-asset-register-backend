package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProducerDTO;
import it.gov.pagopa.register.dto.operation.ProducersResponseDTO;
import it.gov.pagopa.register.service.operation.ProducersService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
  value = ProducersController.class,
  excludeAutoConfiguration = {
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class
  }
)
@AutoConfigureMockMvc(addFilters = false)
class ProducersControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProducersService producersService;

  @Test
  void getProducersByInitiative_shouldReturnPagedProducersWithDefaultSize() throws Exception {
    String initiativeId = "687f8a176a5c92458819922b";
    LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 30);
    LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 11, 45);

    ProducersResponseDTO response = ProducersResponseDTO.builder()
      .content(List.of(ProducerDTO.builder()
        .producerName("Producer 1")
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build()))
      .pageNo(0)
      .pageSize(20)
      .totalElements(1)
      .totalPages(1)
      .build();

    when(producersService.getProducersByInitiative(eq(initiativeId), org.mockito.ArgumentMatchers.any(Pageable.class)))
      .thenReturn(response);

    mockMvc.perform(get("/idpay/register/initiatives/{initiativeId}/producers", initiativeId)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].producerName").value("Producer 1"))
      .andExpect(jsonPath("$.content[0].createdAt").value("2026-01-01T10:30:00"))
      .andExpect(jsonPath("$.content[0].updatedAt").value("2026-01-02T11:45:00"))
      .andExpect(jsonPath("$.pageNo").value(0))
      .andExpect(jsonPath("$.pageSize").value(20))
      .andExpect(jsonPath("$.totalElements").value(1))
      .andExpect(jsonPath("$.totalPages").value(1));

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(producersService).getProducersByInitiative(eq(initiativeId), pageableCaptor.capture());
    assertEquals(20, pageableCaptor.getValue().getPageSize());
  }

  @Test
  void getProducersByInitiative_shouldUseRequestedPageable() throws Exception {
    String initiativeId = "687f8a176a5c92458819922b";

    ProducersResponseDTO response = ProducersResponseDTO.builder()
      .content(List.of())
      .pageNo(2)
      .pageSize(5)
      .totalElements(0)
      .totalPages(0)
      .build();

    when(producersService.getProducersByInitiative(eq(initiativeId), org.mockito.ArgumentMatchers.any(Pageable.class)))
      .thenReturn(response);

    mockMvc.perform(get("/idpay/register/initiatives/{initiativeId}/producers", initiativeId)
        .param("page", "2")
        .param("size", "5")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.pageNo").value(2))
      .andExpect(jsonPath("$.pageSize").value(5));

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(producersService).getProducersByInitiative(eq(initiativeId), pageableCaptor.capture());
    assertEquals(2, pageableCaptor.getValue().getPageNumber());
    assertEquals(5, pageableCaptor.getValue().getPageSize());
  }

}
