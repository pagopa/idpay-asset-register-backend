package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.service.operation.InitiativeService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static it.gov.pagopa.register.constants.ValidationPatterns.ROLE_PATTERN;
import static it.gov.pagopa.register.constants.ValidationPatterns.UUID_V4_PATTERN;

@Validated
@RestController
@RequestMapping("/idpay/register")
@RequiredArgsConstructor
public class InitiativeController {


  private final InitiativeService service;

  @GetMapping("/initiative")
  public ResponseEntity<List<InitiativeDTO>> getInitiativeEnabled(
    @RequestHeader("x-organization-role") @Pattern(regexp = ROLE_PATTERN) String role,
    @RequestHeader("x-organization-id") @Pattern(regexp = UUID_V4_PATTERN) String organizationId
  ) {

    List<InitiativeDTO> initiatives =
      service.getInitiatives(role, organizationId);

    return ResponseEntity.ok(initiatives);
  }
}
