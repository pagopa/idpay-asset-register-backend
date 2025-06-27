package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.model.operation.ProductFile;

public class ProductFileMapper {

  private ProductFileMapper() {}

  public static ProductFileDTO toDTO(ProductFile productFile){
    return ProductFileDTO.builder()
      .idUser(productFile.getIdUser())
      .idUpload(productFile.getIdUpload())
      .uploadDate(productFile.getUploadDate())
      .status(productFile.getStatus())
      .totalUpload(productFile.getTotalUpload())
      .failedUpload(productFile.getFailedUpload())
      .originalFileName(productFile.getOriginalFileName())
      .build();
  }
}
