package it.gov.pagopa.register.model.operation;

import it.gov.pagopa.register.dto.operation.FormalMotivationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Document(collection = "product")

public class Product {

  @Id
  private String gtinCode;
  private String productFileId;
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
  private String capacity;
  private ArrayList<StatusChangeEvent> statusChangeChronology;
  private FormalMotivationDTO formalMotivation = new FormalMotivationDTO("-", LocalDateTime.MIN);
  private String productName;
  private String organizationName;
}
