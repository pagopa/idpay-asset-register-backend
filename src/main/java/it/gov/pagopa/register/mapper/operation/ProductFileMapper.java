package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.ProductBatchDTO;
import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.CATEGORIES_TO_IT;

public class ProductFileMapper {

  private ProductFileMapper() {}

  public static ProductFileDTO toDTO(ProductFile productFile){
    return ProductFileDTO.builder()
      .productFileId(productFile.getId())
      .category(productFile.getCategory())
      .batchName(productFile.getCategory()+"_"+productFile.getId()+".csv")
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
      CATEGORIES_TO_IT.get(product.getCategory()) + "_" + product.getProductFileId() + ".csv"
    );
  }

}
