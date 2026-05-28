package it.gov.pagopa.register.model.operation;

import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "producers_initiative")
public class ProducersInitiative {

  @Id
  private String id;

  private String producerId;
  private String producerEmail;
  private String producerName;
  private String initiativeId;

  private String initiativeName;
  private InitiativeStatus initiativeStatus;

  private LocalDateTime initiativeStartDate;
  private LocalDateTime initiativeEndDate;

  private String initiativeServiceId;
  private String initiativeOrganizationName;

  private Boolean enabled;
  private String source;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
