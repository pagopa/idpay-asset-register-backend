package it.gov.pagopa.register.controller.clean;

import io.swagger.v3.oas.annotations.Hidden;
import it.gov.pagopa.register.exception.role.ConsentNotFoundException;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.service.clean.FixtureCleanupService;
import it.gov.pagopa.register.service.role.PortalConsentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static it.gov.pagopa.register.constants.ValidationPatterns.OBJECT_ID_PATTERN;
import static it.gov.pagopa.register.constants.ValidationPatterns.UUID_V4_PATTERN;

/** Internal fixture cleanup API; enabled only by the test-support flag. */
@RestController
@Hidden
@RequestMapping("/idpay/register/clean/fixtures")
@ConditionalOnProperty(name = "app.test-support.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FixtureCleanupController {
  private final FixtureCleanupService service;
  private final PortalConsentService consents;

  public record FileScope(
      @NotNull @Pattern(regexp = UUID_V4_PATTERN) String organizationId,
      @NotNull @Pattern(regexp = OBJECT_ID_PATTERN) String initiativeId,
      @NotBlank @Size(max = 255) String fileName) { }

  public record ProductScope(
      @NotNull @Pattern(regexp = OBJECT_ID_PATTERN) String initiativeId,
      @NotNull @Size(max = 200) List<@NotBlank String> gtinCodes) { }

  public record ConsentScope(@NotNull @Pattern(regexp = UUID_V4_PATTERN) String userId) { }

  public record AssociationRestore(
      @NotBlank String id, ProducersInitiative before, ProducersInitiative expected) { }

  @GetMapping("/capabilities")
  public List<String> capabilities() {
    return List.of("files-v1", "products-v1", "association-cas-v1", "consents-v1");
  }

  @PostMapping("/products/find")
  public List<Product> products(@RequestBody @Valid ProductScope scope) {
    return service.products(scope);
  }

  @PostMapping("/files/find")
  public List<ProductFile> files(@RequestBody @Valid FileScope scope) {
    return service.files(scope);
  }

  @PostMapping("/files/delete")
  public ResponseEntity<Void> deleteFiles(@RequestBody @Valid FileScope scope) {
    return service.deleteFiles(scope)
        ? ResponseEntity.noContent().build() : ResponseEntity.status(409).build();
  }

  @GetMapping("/associations/{id}")
  public ResponseEntity<ProducersInitiative> association(@PathVariable String id) {
    return service.association(id).map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping("/associations/restore")
  public ResponseEntity<Void> restore(@RequestBody @Valid AssociationRestore request) {
    return service.restore(request)
        ? ResponseEntity.noContent().build() : ResponseEntity.status(409).build();
  }

  @PostMapping("/consents/delete")
  public ResponseEntity<Void> deleteConsent(@RequestBody @Valid ConsentScope scope) {
    try {
      consents.remove(scope.userId());
    } catch (ConsentNotFoundException _) {
      // A read-only consent scenario has no acceptance document to remove.
    }
    return ResponseEntity.noContent().build();
  }
}
