package it.gov.pagopa.register.dto.operation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeDTO {

  private String initiativeId;
  private String initiativeName;
  private String organizationName;

  private InitiativeStatus status;

  private LocalDate startDate;
  private LocalDate endDate;

  private String serviceId;
  private Boolean enabled;
}
