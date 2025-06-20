package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.onetrust.InitiativeFileStorageClient;
import it.gov.pagopa.register.constants.enums.UploadCsvStatus;
import it.gov.pagopa.register.dto.operation.RegisterUploadReqeustDTO;
import it.gov.pagopa.register.exception.operation.CsvValidationException;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.UploadCsv;
import it.gov.pagopa.register.repository.operation.UploadRepository;
import it.gov.pagopa.register.utils.ReportError;
import it.gov.pagopa.register.utils.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.register.utils.Utils.*;


@Service
@Slf4j
public class ProductService {

  private final UploadRepository uploadRepository;
  private final InitiativeFileStorageClient azureBlobClient;

  public ProductService(UploadRepository uploadRepository, InitiativeFileStorageClient azureBlobClient) {
    this.uploadRepository = uploadRepository;
    this.azureBlobClient = azureBlobClient;
  }


  public void saveCsv(RegisterUploadReqeustDTO registerUploadReqeustDTO, String idOrg, String idUser, String role) {
        /*
          1. CONTROLLI CHE BLOCCO IL CONTROLLO DEL CONTENUTO

          1. Controllo estensione
          2. Controllo prima riga come header
          3. Controllo numero righe csvParser.getRecords().size();
         */

    //2. CONTROLLI DEL CONTENUTO

    //1.1

    //check estensione
    if (!isCsv(registerUploadReqeustDTO.getCsv()))
      throw new CsvValidationException("Il file inserito non è un .csv");

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(registerUploadReqeustDTO.getCsv().getInputStream()));







         //settare ; come separator
         CSVParser csvParser = new CSVParser(reader,
           CSVFormat.Builder.create().setHeader().setTrim(true).build())) {






      Boolean isPianoCottura = checkCategory(CATEGORIE_PIANI_COTTURA, registerUploadReqeustDTO.getCategory());
      //check headers
      if ((isPianoCottura && !CSV_HEADER_PIANI_COTTURA.containsAll(csvParser.getHeaderMap().keySet()))
        || (!isPianoCottura && !CSV_HEADER_PRODOTTI.containsAll(csvParser.getHeaderMap().keySet()))) {
        throw new CsvValidationException("header csv non validi");
      }


      //check numero righe
      //da spostare nell'appProperties
      if (csvParser.getRecords().size() > MAX_ROWS + 1)
        throw new CsvValidationException("numero di record nel csv maggiore di " + MAX_ROWS);

      //GENERARE idUpload idOrg +category+ userId + timestamp
      String idUpload = idOrg + "-" + registerUploadReqeustDTO.getCategory() + "-" + idUser + "-" +LocalDateTime.now().toString();


      //Check contenuto
      ReportError errors = new ReportError(idUpload, new ArrayList<>());
      List <String> messageErrors = new ArrayList<>();

      List<String> errorHeaders = new ArrayList<>();
      if(isPianoCottura){
        errorHeaders.addAll(CSV_HEADER_PIANI_COTTURA);
      }else{
        errorHeaders = new ArrayList(CSV_HEADER_PRODOTTI);
      }

      errorHeaders.add("Errori");

      List errorRecords = new ArrayList<>();


      for (CSVRecord record : csvParser) {

        if (isPianoCottura) {
          List<ValidationError> errorList = checkPianiCotturaCsvRow(record);
          errors.getErrors().addAll(errorList);
        } else {
          List<ValidationError> errorList = checkProdottiCsvRow(record);
          errors.getErrors().addAll(errorList);
        }
      }

      String fileName = registerUploadReqeustDTO.getCsv().getResource().getFile().getName();

      //da inserire dopo iterazione
      UploadCsv uploadFile = new UploadCsv(
        idUser,
        idOrg,
        idUpload,
        LocalDateTime.now(),
        UploadCsvStatus.LOADING_CHECK.toString(),
        null,
        null,
        fileName);



      if (errors.getErrors().isEmpty()) {

        // 1.1 Salvo a db i dati legati al caricamento è quelli necessari al successivo downlaod
        uploadFile.setStatus(UploadCsvStatus.FORMAL_OK.toString());
        uploadRepository.save(uploadFile);


        //1.2 Carico il file sullo storage di azure
        String destination = "/CSV/" + idOrg + "/";
        azureBlobClient.uploadFile(registerUploadReqeustDTO.getCsv().getResource().getFile(),
          destination,
          registerUploadReqeustDTO.getCsv().getContentType());

                /*
                1.3 Restituo il seguento body e code
                String idUpload = ID GENERATO IN PRECEDENZA
                String statusUpload = QUALCOSA DEL TIPO PRESA IN CARICO;
                Status 200
               */
      } else {

        // 2.1 Salvo a db i dati legati al caricamento è quelli necessari al successivo downlaod
        uploadFile.setStatus(UploadCsvStatus.FORMAL_KO.toString());
        uploadRepository.save(uploadFile);

        //2.2 Geniro il file di report e carico il file sullo storage di azure

        String destination = "/Report/Eprel_Error/";
        // ReportError da cambiare in MultipartFile? (1 riga per prodotto)
        azureBlobClient.uploadFile(null,
          destination,
          registerUploadReqeustDTO.getCsv().getContentType());



             /*
                2.3 Restituo il seguento body e code
                String idUpload = ID GENERATO IN PRECEDENZA
                String statusUpload = VALIDATION_ERROR;
                Status 500
              */

      }


    } catch (IOException e) {
      throw new RuntimeException("Errore nella lettura del file CSV: " + e.getMessage());
    }
  }

  private Boolean checkCategory(List<String> listaCategorie, String categoria){
    return listaCategorie.contains(categoria);

  }

  private Boolean isCsv(MultipartFile file){
    if (file == null || file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().toLowerCase().endsWith(".csv") ) {
      return false;
    }

    return true;
  }

  private List<ValidationError> checkPianiCotturaCsvRow(CSVRecord csvRecord) {
    List<ValidationError> errors = new ArrayList<>();
    int rowCount = 0;
    if (!csvRecord.get("Codice GTIN/EAN").matches(CODICE_GTIN_EAN_REGEX)) {
      errors.add(new ValidationError(rowCount, "codiceGTIN/EAN", "Il Codice GTIN/EAN è obbligatorio, deve essere univoco, alfanumerico e lungo al massimo 14 caratteri."));
    }
    if (!CATEGORIE_PIANI_COTTURA.contains(csvRecord.get("Categoria"))) {
      errors.add(new ValidationError(rowCount, "categoria", "Il campo Categoria è obbligatorio e deve contenere il valore fisso 'COOKINGHOBS'."));
    }
    if (!csvRecord.get("Marca").matches(MARCA_REGEX)) {
      errors.add(new ValidationError(rowCount, "marca", "Il campo Marca è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri."));
    }
    if (!csvRecord.get("Modello").matches(MODELLO_REGEX)) {
      errors.add(new ValidationError(rowCount, "modello", "Il campo Modello è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri."));
    }
    if (!csvRecord.get("Codice prodotto").matches(CODICE_PRODOTTO_REGEX)) {
      errors.add(new ValidationError(rowCount, "codiceProdotto", "Il Codice prodotto non deve contenere caratteri speciali (come &, ', <, >, %, $, :, ;, =) o lettere accentate e deve essere lungo al massimo 100 caratteri."));
    }
    if (!csvRecord.get("Paese di Produzione").matches(PAESE_DI_PRODUZIONE_REGEX)) {
      errors.add(new ValidationError(rowCount, "codiceProdotto", "Il Paese di Produzione è obbligatorio e deve essere composto da esattamente 2 caratteri"));
    }
    return errors;
  }

  private List<ValidationError> checkProdottiCsvRow(CSVRecord csvRecord) {
    List<ValidationError> errors = new ArrayList<>();
    int rowCount = 0;
    if (!csvRecord.get("Codice EPREL").matches(CODICE_EPREL_REGEX)) {
      errors.add(new ValidationError(rowCount, "codiceEPREL", "Il Codice EPREL è obbligatorio e deve essere un valore numerico."));
    }
    if (!csvRecord.get("Codice GTIN/EAN").matches(CODICE_GTIN_EAN_REGEX)) {
      errors.add(new ValidationError(rowCount, "codiceGTIN/EAN", "Il Codice GTIN/EAN è obbligatorio, deve essere univoco, alfanumerico e lungo al massimo 14 caratteri."));
    }
    if (!CATEGORIE_PRODOTTI.contains(csvRecord.get("Categoria"))) {
      errors.add(new ValidationError(rowCount, "categoria", "Il campo Categoria è obbligatorio e deve contenere uno dei seguenti valori: WASHINGMACHINES, WASHDRYERS, OVENS, RANGEHOODS, DISHWASHERS, TUMBLEDRIERS, REFRIGERATINGAPPL."));
    }
    if (!csvRecord.get("Codice prodotto").matches(CODICE_PRODOTTO_REGEX)) {
      errors.add(new ValidationError(rowCount, "codiceProdotto", "Il Codice prodotto non deve contenere caratteri speciali (come &, ', <, >, %, $, :, ;, =) o lettere accentate e deve essere lungo al massimo 100 caratteri."));
    }
    if (!csvRecord.get("Paese di Produzione").matches(PAESE_DI_PRODUZIONE_REGEX)) {
      errors.add(new ValidationError(rowCount, "codiceProdotto", "Il Paese di Produzione è obbligatorio e deve essere composto da esattamente 2 caratteri"));
    }
    return errors;
  }


  public ByteArrayOutputStream downloadReport(String idUpload) {
    UploadCsv upload = uploadRepository.findByIdUpload(idUpload)
      .orElseThrow(() -> new ReportNotFoundException("Report non trovato con id: " + idUpload));

    String filePath = "";

    // 2. Ricavo il percorso del file da Azure
    //String filePath = upload.getStoragePath();
    //Controllo se è un formalError o eprelError
    //In base al tipo costruisco il percorso
    if (upload.getStatus().equalsIgnoreCase("EPREL_KO")) {
      filePath = "Report/Eprel_Error/" + upload.getIdUpload() + ".csv";
    } else if (upload.getStatus().equalsIgnoreCase("FORMAL_KO")) {
      filePath = "Report/Formal_Error/" + upload.getIdUpload() + ".csv";
    } else {
      throw new ReportNotFoundException("Tipo di errore non supportato: " + upload.getStatus());
    }


    // 4. Invoco il download
    ByteArrayOutputStream result = azureBlobClient.download(filePath);
    if (result == null) {
      throw new ReportNotFoundException("Report non trovato su Azure per path: " + filePath);
    }
    return result;
  }
}


