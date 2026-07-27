package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProducersResponseDTO;
import it.gov.pagopa.register.service.operation.ProducersService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/idpay/register/initiatives/{initiativeId}")
@RequiredArgsConstructor
public class ProducersController {

  private final ProducersService producersService;

  @GetMapping("/producers")
  public ResponseEntity<ProducersResponseDTO> getProducersByInitiative(
    @PathVariable("initiativeId") String initiativeId,
    @PageableDefault(size = 1000) Pageable pageable
  ) {
    return ResponseEntity.ok(producersService.getProducersByInitiative(initiativeId, pageable));
  }
}
