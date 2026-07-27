package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.ProductBatchDTO;
import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.Category.Labels.CATEGORIES_FOR_DTO;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.Category.Labels.CATEGORIES_FOR_FILENAME;

public class ProductFileMapper {

  private ProductFileMapper() {}

  public static ProductFileDTO toDTO(ProductFile productFile){
    return ProductFileDTO.builder()
      .productFileId(productFile.getId())
      .category(CATEGORIES_FOR_DTO.get(productFile.getCategory()))
      .batchName(CATEGORIES_FOR_FILENAME.get(productFile.getCategory())+"_"+productFile.getId()+".csv")
      .fileName(productFile.getFileName())
      .uploadStatus(productFile.getUploadStatus())
      .dateUpload(productFile.getDateUpload())
      .findedProductsNumber(productFile.getFindedProductsNumber())
      .addedProductNumber(productFile.getAddedProductNumber())
      .build();
  }

  public static ProductBatchDTO toBatchDTO(Product product) {
    return new ProductBatchDTO(
      product.getProductFileId(),
      CATEGORIES_FOR_FILENAME.get(product.getCategory()) + "_" + product.getProductFileId() + ".csv"
    );
  }

}
