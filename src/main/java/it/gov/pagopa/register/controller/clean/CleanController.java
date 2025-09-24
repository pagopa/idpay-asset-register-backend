package it.gov.pagopa.register.controller.clean;

import it.gov.pagopa.register.dto.clean.CleanRequestDTO;
import it.gov.pagopa.register.service.clean.CleanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/idpay/register/clean")
@RequiredArgsConstructor
public class CleanController {


  private final CleanService cleanService;


  @PostMapping("/products")
  public ResponseEntity<Void> removeProducts(@RequestBody CleanRequestDTO cleanRequestDTO) {
    cleanService.removeProducts(cleanRequestDTO.getIds());
    return ResponseEntity.noContent().build();
  }


  @PostMapping("/products-file")
  public ResponseEntity<Void> removeProductsFile(@RequestBody CleanRequestDTO cleanRequestDTO){
    cleanService.removeProductsFile(cleanRequestDTO.getIds());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/products-file/report")
  public ResponseEntity<Void> removeReportFileFromStorage( @RequestBody CleanRequestDTO cleanRequestDTO){
    cleanService.removeReportFileFromStorage(cleanRequestDTO.getIds());
    return ResponseEntity.noContent().build();
  }
  @PostMapping("/products-file/formal")
  public ResponseEntity<Void> removeFormalFileFromStorage(@RequestBody CleanRequestDTO cleanRequestDTO){
    cleanService.removeFormalFileFromStorage(cleanRequestDTO.getIds());
    return ResponseEntity.noContent().build();
  }
}
