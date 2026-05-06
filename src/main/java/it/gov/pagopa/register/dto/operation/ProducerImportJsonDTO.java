package it.gov.pagopa.register.dto.operation;

import lombok.Data;

@Data
public class ProducerImportJsonDTO {
  private String id;
  private String producerId;
  private String initiativeId;
  private String initiativeName;
  private String initiativeStatus;
  private String initiativeStartDate;
  private String initiativeEndDate;
  private String initiativeServiceId;
  private String initiativeOrganizationName;
  private Boolean enabled;
  private String source;
  private String createdAt;
  private String updatedAt;
}
