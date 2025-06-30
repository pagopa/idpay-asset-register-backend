package it.gov.pagopa.register.connector.storage;

import it.gov.pagopa.common.storage.AzureBlobClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class FileStorageClient extends AzureBlobClientImpl {


  FileStorageClient(@Value("${blobStorage.connectionString}") String storageConnectionString,
                    @Value("${blobStorage.initiative.logo.containerReference}") String institutionsLogoContainerReference) {
    super(storageConnectionString, institutionsLogoContainerReference);
  }

}
