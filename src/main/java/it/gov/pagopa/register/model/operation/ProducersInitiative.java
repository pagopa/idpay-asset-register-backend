package it.gov.pagopa.register.model.operation;

import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "producers_initiative")
public class ProducersInitiative {

  @Id
  private String id;

  private String producerId;
  private String initiativeId;

  private String initiativeName;
  private InitiativeStatus initiativeStatus;

  private LocalDate initiativeStartDate;
  private LocalDate initiativeEndDate;

  private String initiativeServiceId;
  private String initiativeOrganizationName;

  private Boolean enabled;
  private String source;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
