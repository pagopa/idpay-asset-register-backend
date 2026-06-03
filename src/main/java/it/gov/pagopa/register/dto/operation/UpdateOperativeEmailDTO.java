package it.gov.pagopa.register.dto.operation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static it.gov.pagopa.register.constants.ValidationPatterns.EMAIL_PATTERN;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOperativeEmailDTO {

  @NotBlank(message = "Missing or empty 'operativeEmail' field in request body")
  @Pattern(regexp = EMAIL_PATTERN, message = "Invalid email format")
  private String operativeEmail;
}
