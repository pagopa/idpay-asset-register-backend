package it.gov.pagopa.register.model.operation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document("product_file")
@NoArgsConstructor
@AllArgsConstructor
public class ProductFile {
  private String productFileId;
  private String userId;
  private String organizationId;
  private String uploadId;
  private String fileName;
  private String uploadStatus;
  private LocalDateTime dateUpload;
  private Integer findedProductsNumber;
  private Integer addedProductNumber;
  private String reportProducts;

  public ProductFile(String fileName) {
    this.fileName = fileName;
  }
}
