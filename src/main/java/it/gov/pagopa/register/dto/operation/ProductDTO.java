package it.gov.pagopa.register.dto.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

  @Id
  private String gtinCode;
  private String organizationId;
  private LocalDateTime registrationDate;
  private String status;
  private String model;
  private String productGroup;
  private String category;
  private String brand;
  private String eprelCode;
  private String productCode;
  private String countryOfProduction;
  private String energyClass;
  private String linkEprel;
  private String batchName;

}
