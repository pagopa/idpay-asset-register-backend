package it.gov.pagopa.register.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfig {

  @Bean
  public BlobContainerClient fileStorageClientConfiguration(
    @Value("${blobStorage.connectionString}") String connectionString,
    @Value("${blobStorage.initiative.logo.containerReference}") String containerName) {

    return new BlobServiceClientBuilder()
      .connectionString(connectionString)
      .buildClient()
      .getBlobContainerClient(containerName);
  }
}
