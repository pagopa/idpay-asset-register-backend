package it.gov.pagopa.register.dto.mapper.operation;

import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.model.operation.ProductFile;

public class ProductFileMapper {

  private ProductFileMapper() {}

  public static ProductFileDTO toDTO(ProductFile upload){
    return ProductFileDTO.builder()
      .idUser(upload.getIdUser())
      .idUpload(upload.getIdUpload())
      .uploadDate(upload.getUploadDate())
      .status(upload.getStatus())
      .totalUpload(upload.getTotalUpload())
      .failedUpload(upload.getFailedUpload())
      .originalFileName(upload.getOriginalFileName())
      .build();
  }
}
