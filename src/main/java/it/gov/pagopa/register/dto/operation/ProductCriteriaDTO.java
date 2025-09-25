package it.gov.pagopa.register.dto.operation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCriteriaDTO {
  private String organizationId;
  private String category;
  private String productFileId;
  private String eprelCode;
  private String gtinCode;
  private String productName;
  private String brand;
  private String model;
  private String status;
}
