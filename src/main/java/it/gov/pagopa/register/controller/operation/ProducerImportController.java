package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.dto.operation.ProducerInitiativeImportRequestDTO;
import it.gov.pagopa.register.service.operation.ProducerImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/idpay/register")
@RequiredArgsConstructor
public class ProducerImportController {

  private final ProducerImportService producerImportService;

  @PostMapping(value = "/producers", consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<ProducerImportResultDTO> importProducers(
    @RequestBody ProducerInitiativeImportRequestDTO request
  ) {
    return ResponseEntity.ok(producerImportService.importProducers(request.getProducers()));
  }
}
