package it.gov.pagopa.register.dto.operation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductFileDTO {
  private String productFileId;
  private String category;
  private String fileName;
  private String batchName;
  private String uploadStatus;
  private LocalDateTime dateUpload;
  private Integer findedProductsNumber;
  private Integer addedProductNumber;
}
