package it.gov.pagopa.register.constants;

import it.gov.pagopa.register.dto.operation.KeyDTO;

public final class UploadKeyConstant {

  public static final KeyDTO EXTENSION_FILE_ERROR = new KeyDTO("product.invalid.file.extension",
    "header csv non validi");


  public static final KeyDTO MAX_ROW_FILE_ERROR = new KeyDTO("product.invalid.file.maxrow",
    "numero di record nel csv maggiore di 100");


  public static final KeyDTO HEADER_FILE_ERROR = new KeyDTO("product.invalid.file.header",
    "header csv non validi");


  public static final KeyDTO REPORT_FORMAL_FILE_ERROR = new KeyDTO("product.invalid.file.report",
    "il file caricato ha generato un report file di errori");


  public static final KeyDTO UPLOAD_FILE_OK = new KeyDTO("product.valid.file",
    "caricamento eseguito correttamente");

}
