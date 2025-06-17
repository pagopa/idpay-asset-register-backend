package it.gov.pagopa.register.connector.onetrust;

import it.gov.pagopa.common.storage.AzureBlobClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class InitiativeFileStorageClient extends AzureBlobClientImpl implements InitiativeFileStorageConnector {


  InitiativeFileStorageClient(@Value("${blobStorage.connectionString}") String storageConnectionString,
                              @Value("${blobStorage.initiative.logo.containerReference}") String institutionsLogoContainerReference) {
    super(storageConnectionString, institutionsLogoContainerReference);
  }

  @Override
  public void uploadInitiativeLogo(InputStream file, String fileName, String contentType) {
    upload(file, fileName, contentType);
  }

  @Override
  public ByteArrayOutputStream downloadInitiativeLogo(String fileName) {
    return download(fileName);
  }
}
