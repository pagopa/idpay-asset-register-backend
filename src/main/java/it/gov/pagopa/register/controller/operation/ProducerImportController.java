package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.service.operation.ProducerImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/idpay/register")
@RequiredArgsConstructor
public class ProducerImportController {

  private final ProducerImportService producerImportService;

  @PostMapping(value = "/producers/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProducerImportResultDTO> importProducers(@RequestPart("csv") MultipartFile csv) {
    return ResponseEntity.ok(producerImportService.importCsv(csv));
  }

  @PostMapping(value = "/producers", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/x-ndjson"})
  public ResponseEntity<ProducerImportResultDTO> importProducers(@RequestBody String json) {
    return ResponseEntity.ok(producerImportService.importJson(json));
  }
}
