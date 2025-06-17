package it.gov.pagopa.register.connector.onetrust;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public interface InitiativeFileStorageConnector {
  void uploadInitiativeLogo(InputStream file, String fileName, String contentType);
  ByteArrayOutputStream downloadInitiativeLogo(String fileName);
}
