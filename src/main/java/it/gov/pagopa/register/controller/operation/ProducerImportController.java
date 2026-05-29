package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.dto.operation.UpdateOperativeEmailDTO;
import it.gov.pagopa.register.dto.operation.UpdatedOperativeEmailResult;
import it.gov.pagopa.register.service.operation.ProducerImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static it.gov.pagopa.register.constants.ValidationPatterns.OBJECT_ID_PATTERN;
import static it.gov.pagopa.register.constants.ValidationPatterns.UUID_V4_PATTERN;

@Validated
@RestController
@RequestMapping("/idpay/register")
@RequiredArgsConstructor
public class ProducerImportController {

  private final ProducerImportService producerImportService;

  @PostMapping(value = "/producers", consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<ProducerImportResultDTO> importProducers(@RequestBody String json) {
    return ResponseEntity.ok(producerImportService.importJson(json));
  }

  @PutMapping("/initiatives/{initiativeId}/email")
  public ResponseEntity<UpdatedOperativeEmailResult> updateOperativeEmail(
    @RequestHeader("x-organization-id") @Pattern(regexp = UUID_V4_PATTERN) String organizationId,
    @PathVariable("initiativeId") @Pattern(regexp = OBJECT_ID_PATTERN) String initiativeId,
    @Valid @RequestBody UpdateOperativeEmailDTO body
  ) {
    UpdatedOperativeEmailResult result = producerImportService.updateOperativeEmail(organizationId, initiativeId, body.getOperativeEmail());
    return ResponseEntity.ok(result);
  }

}
