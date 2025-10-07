package it.gov.pagopa.register.dto.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDTO {

  @Id
  private String gtinCode;
  private String organizationId;
  private String registrationDate;
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
  private String productName;
  private String capacity;
  private List<StatusChangeEvent> statusChangeChronology;
  private String formalMotivation;
  private String organizationName;
}
