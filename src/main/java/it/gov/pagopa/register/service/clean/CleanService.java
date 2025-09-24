package it.gov.pagopa.register.service.clean;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
@Service
public class CleanService {

  private final ProductRepository productRepository;
  private final ProductFileRepository productFileRepository;
  private final FileStorageClient fileStorageClient;

  public CleanService(ProductRepository productRepository, ProductFileRepository productFileRepository, FileStorageClient fileStorageClient) {
    this.productRepository = productRepository;
    this.productFileRepository = productFileRepository;
    this.fileStorageClient = fileStorageClient;
  }


  public void removeProducts(List<String> ids) {
    productRepository.deleteAllById(ids);
  }

  public void removeProductsFile(List<String> ids){
    productFileRepository.deleteAllById(ids);
  }

  public void removeReportFileFromStorage(List<String> ids){
    ids.forEach(id -> fileStorageClient.deleteFile( REPORT_PARTIAL_ERROR + id + CSV));
  }
  public void removeFormalFileFromStorage(List<String> ids){
    ids.forEach(id -> fileStorageClient.deleteFile( REPORT_FORMAL_ERROR + id + CSV));
  }


}
