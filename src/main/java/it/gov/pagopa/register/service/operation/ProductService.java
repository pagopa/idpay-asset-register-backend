package it.gov.pagopa.register.service.operation;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.models.BlobProperties;
import it.gov.pagopa.common.storage.AzureBlobClientImpl;
import it.gov.pagopa.register.connector.onetrust.InitiativeFileStorageClient;
import it.gov.pagopa.register.connector.onetrust.InitiativeFileStorageConnector;
import it.gov.pagopa.register.dto.operation.RegisterUploadReqeustDTO;
import it.gov.pagopa.register.exception.operation.CsvValidationException;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.UploadCsv;
import it.gov.pagopa.register.repository.operation.UploadRepository;
import it.gov.pagopa.register.utils.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

  private final List<String> categorieProdotti = List.of(
    "WASHINGMACHINES",
    "WASHERDRIERS",
    "OVENS",
    "RANGEHOODS",
    "DISHWASHERS",
    "TUMBLEDRIERS",
    "REFRIGERATINGAPPL"
  );


  public void saveCsv(RegisterUploadReqeustDTO registerUploadReqeustDTO) {
        /*
          1. CONTROLLI CHE BLOCCO IL CONTROLLO DEL CONTENUTO

          1. Controllo estensione
          2. Controllo prima riga come header
          3. Controllo numero righe csvParser.getRecords().size();
         */

        //2. CONTROLLI DEL CONTENUTO

        //1.1

        //check estensione
        if(!isCsv(registerUploadReqeustDTO.getCsv()))
              throw new CsvValidationException("Il file inserito non è un .csv");
        List<List<ValidationError>> validationErrors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(registerUploadReqeustDTO.getCsv().getInputStream()));
             CSVParser csvParser = new CSVParser(reader,
                     CSVFormat.Builder.create().setHeader().setTrim(true).build()))  {

          //check headers
          if (!csvParser.getHeaderMap().keySet().containsAll(CSV_HEADER)) {
            throw new CsvValidationException("header csv non validi");
          }

          //check numero righe
          if(csvParser.getRecords().size() > MAX_ROWS+1)
            throw new CsvValidationException("numero di record nel csv maggiore di " + MAX_ROWS);

          //GENERARE idUpload
          UUID idUpload = UUID.randomUUID();


          //Check contenuto
          for (CSVRecord record : csvParser) {
            String categoriaValue = record.get("Categoria");
            if ("COOKINGHOBS".equalsIgnoreCase(categoriaValue)) {
              validationErrors.add(checkPianiCotturaCsvRow(record));
            }else{
              validationErrors.add(checkProdottiCsvRow(record));
            }
          }


          for(List<ValidationError> validationError : validationErrors){


            //1 SE validationErrors è VUOTA
            if (validationError.isEmpty()){
              // 1.1 Salvo a db i dati legati al caricamento è quelli necessari al successivo downlaod
              uploadRepository.save(new UploadCsv());

              //1.2 Carico il file sullo storage di azure
              String destination = "";
              azureBlobClient.uploadFile(registerUploadReqeustDTO.getCsv().getResource().getFile(),
                destination,
                registerUploadReqeustDTO.getCsv().getContentType());
              /*
                1.3 Restituo il seguento body e code
                String idUpload = ID GENERATO IN PRECEDENZA
                String statusUpload = QUALCOSA DEL TIPO PRESA IN CARICO;
                Status 200
               */



              //2 SE validationErrors NON è VUOTA
            }else{
              // 2.1 Salvo a db i dati legati al caricamento è quelli necessari al successivo downlaod
              uploadRepository.save(new UploadCsv());

              //2.2 Geniro il file di report e carico il file sullo storage di azure


             /*
                2.3 Restituo il seguento body e code
                String idUpload = ID GENERATO IN PRECEDENZA
                String statusUpload = VALIDATION_ERROR;
                Status 500
              */



            }

          }

        } catch ( IOException e) {
            throw new RuntimeException("Errore nella lettura del file CSV: " + e.getMessage());
        }
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
                errors.add(new ValidationError(rowCount, "codiceGTIN/EAN", "Il campo codiceGTIN/EAN non rispetta la regex: " + CODICE_GTIN_EAN_REGEX));
            }
            if (!csvRecord.get("Categoria").equals("COOKINGHOBS")) {
                errors.add(new ValidationError(rowCount, "categoria", "Il campo categoria è diverso da COOKINGHOBS"));
            }
            if (!csvRecord.get("Marca").matches(MARCA_REGEX)) {
                errors.add(new ValidationError(rowCount, "marca", "Il campo marca non rispetta la regex: " + MARCA_REGEX));
            }
            if (!csvRecord.get("Modello").matches(MODELLO_REGEX)) {
                errors.add(new ValidationError(rowCount, "modello", "Il campo modello non rispetta la regex: " + MODELLO_REGEX));
            }
            if (!csvRecord.get("Codice prodotto").matches(CODICE_PRODOTTO_REGEX)) {
                errors.add(new ValidationError(rowCount, "codiceProdotto", "Il campo codiceProdotto non rispetta la regex: " + CODICE_PRODOTTO_REGEX));
            }
            if (!csvRecord.get("Paese di Produzione").matches(PAESE_DI_PRODUZIONE_REGEX)) {
              errors.add(new ValidationError(rowCount, "codiceProdotto", "Il campo codiceProdotto non rispetta la regex: " + CODICE_PRODOTTO_REGEX));
            }
        return errors;
    }

    private List<ValidationError> checkProdottiCsvRow(CSVRecord csvRecord) {
      List<ValidationError> errors = new ArrayList<>();
      int rowCount = 0;
      if (!csvRecord.get("Codice EPREL").matches(CODICE_EPREL_REGEX)) {
        errors.add(new ValidationError(rowCount, "codiceGTIN/EAN", "Il campo codiceGTIN/EAN non rispetta la regex: " + CODICE_GTIN_EAN_REGEX));
      }
      if (!csvRecord.get("Codice GTIN/EAN").matches(CODICE_GTIN_EAN_REGEX)) {
        errors.add(new ValidationError(rowCount, "codiceGTIN/EAN", "Il campo codiceGTIN/EAN non rispetta la regex: " + CODICE_GTIN_EAN_REGEX));
      }
      if (!categorieProdotti.contains(csvRecord.get("Categoria"))) {
        errors.add(new ValidationError(rowCount, "categoria", "Il campo categoria non rispetta la lista categorie prodotto"));
      }
      if (!csvRecord.get("Codice prodotto").matches(CODICE_PRODOTTO_REGEX)) {
        errors.add(new ValidationError(rowCount, "codiceProdotto", "Il campo codiceProdotto non rispetta la regex: " + CODICE_PRODOTTO_REGEX));
      }
      if (!csvRecord.get("Paese di Produzione").matches(PAESE_DI_PRODUZIONE_REGEX)) {
        errors.add(new ValidationError(rowCount, "codiceProdotto", "Il campo codiceProdotto non rispetta la regex: " + CODICE_PRODOTTO_REGEX));
      }
      return errors;
    }


  public Path downloadReport(String idUpload) throws IOException {
    // 1. Recupero il metadata dal DB
    UploadCsv upload = uploadRepository.findByIdUpload(idUpload)
      .orElseThrow(() -> new ReportNotFoundException("Report non trovato con id: " + idUpload));

    String filePath = "";

    // 2. Ricavo il percorso del file da Azure
    //String filePath = upload.getStoragePath();
    //Controllo se è un formalError o eprelError
    //In base al tipo costruisco il percorso
    if (upload.getStatus().equalsIgnoreCase("EPREL_ERROR")) {
      filePath = "Report/Eprel_Error/" + upload.getIdUpload() + ".csv";
    } else if (upload.getStatus().equalsIgnoreCase("FORMAL_ERROR")) {
      filePath = "Report/Formal_Error/" + upload.getIdUpload() + ".csv";
    } else {
      throw new ReportNotFoundException("Tipo di errore non supportato: " + upload.getStatus());
    }

    // 3. Preparo una destinazione temporanea
    Path destination = Paths.get(System.getProperty("java.io.tmpdir"), upload.getIdUpload().toString());

    // 4. Invoco il download
    Response<BlobProperties> result = azureBlobClient.download(filePath, destination);
    if (result == null) {
      throw new ReportNotFoundException("Report non trovato su Azure per path: " + filePath);
    }
    return destination;
  }
}


