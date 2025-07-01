package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.model.operation.ProductFile;

public class ProductFileMapper {

  private ProductFileMapper() {}

  public static ProductFileDTO toDTO(ProductFile productFile){
    return ProductFileDTO.builder()
      .productFileId(productFile.getId())
      .category(productFile.getCategory())
      .batchName(productFile.getCategory()+"-"+productFile.getId())
      .fileName(productFile.getFileName())
      .uploadStatus(productFile.getUploadStatus())
      .dateUpload(productFile.getDateUpload())
      .findedProductsNumber(productFile.getFindedProductsNumber())
      .addedProductNumber(productFile.getAddedProductNumber())
      .build();
  }
}
