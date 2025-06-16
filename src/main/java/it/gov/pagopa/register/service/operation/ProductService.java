package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.common.storage.AzureBlobClientImpl;
import it.gov.pagopa.register.dto.operation.RegisterUploadReqeustDTO;
import it.gov.pagopa.register.exception.operation.CsvValidationException;
import it.gov.pagopa.register.repository.operation.UploadRepository;
import it.gov.pagopa.register.utils.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.register.utils.Utils.*;


@Service
@Slf4j
public class ProductService {

  private final UploadRepository uploadRepository;
  private final AzureBlobClientImpl azureBlobClient;

  public ProductService(UploadRepository uploadRepository, AzureBlobClientImpl azureBlobClient) {
    this.uploadRepository = uploadRepository;
    this.azureBlobClient = azureBlobClient;
  }


  public void saveCsv(RegisterUploadReqeustDTO registerUploadReqeustDTO) {
        /*
          1. CONTROLLI CHE BLOCCO IL CONTROLLO DEL CONTENUTO

          1. Controllo estensione
          2. Controllo prima riga come header
          3. Controllo numero righe csvParser.getRecords().size();
         */

        //2. CONTROLLI DEL CONTENUTO

        //1.1
        if(!registerUploadReqeustDTO.getCsv().getContentType().equalsIgnoreCase("csv"))
              throw new CsvValidationException("MESSAGGIO DA DEFINIRE");
        List<ValidationError> validationErrors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(registerUploadReqeustDTO.getCsv().getInputStream()));
             CSVParser csvParser = new CSVParser(reader,
                     CSVFormat.Builder.create().build().builder().setHeader().setTrim(true).build()))  {
             /*1.2
              if(!checkHeaders())
                  throw new CsvValidationException("MESSAGGIO DA DEFINIRE");
              */
             //1.3
             if(csvParser.getRecords().size() > MAX_ROWS)
               throw new CsvValidationException("MESSAGGIO DA DEFINIRE");

             //GENERARE idUpload

             //2
             for (CSVRecord csvRecord : csvParser) {
                      validationErrors.addAll(checkCsvRow(csvRecord));
             }
             /*
              SE validationErrors è VUOTA
                1.
                1.1 Salvo a db i dati legati al caricamento è quelli necessari al successivo downlaod
                1.2 Carico il file sullo storage di azure
                1.3 Restituo il seguento body e code
                String idUpload = ID GENERATO IN PRECEDENZA
                String statusUpload = QUALCOSA DEL TIPO PRESA IN CARICO;
                Status 200

                2.
                2.1 Salvo a db i dati legati al caricamento è quelli necessari al successivo downlaod
                2.2 Geniro il file di reporr e carico il file sullo storage di azure
                1.3 Restituo il seguento body e code
                String idUpload = ID GENERATO IN PRECEDENZA
                String statusUpload = VALIDATION_ERROR;
                Status 500
              */
        } catch ( IOException e) {
            throw new RuntimeException("Errore nella lettura del file CSV: " + e.getMessage());
        }
    }

    private boolean checkHeaders(CSVRecord csvRecord, String category){
      if(category.equalsIgnoreCase("pianicottura"))
        // CONTROLLO HEADER CSV PIANI COTTURA
        return true;
      else {
        // CONTROLLO HEADER CSV EPREL
        return true;
      }
    }
    private List<ValidationError> checkCsvRow(CSVRecord csvRecord) {
            List<ValidationError> errors = new ArrayList<>();
            int rowCount = 0;
            if (!csvRecord.get("codiceEprel").matches(CODICE_GTIN_REGEX)) {
                errors.add(new ValidationError(rowCount, "codiceGTIN", "Il campo codiceGTIN non rispetta la regex: " + CODICE_GTIN_REGEX));
            }
            if (!csvRecord.get("categoria").matches(CATEGORIA_REGEX)) {
                errors.add(new ValidationError(rowCount, "categoria", "Il campo categoria non rispetta la regex: " + CATEGORIA_REGEX));
            }
            if (!csvRecord.get("marca").matches(MARCA_REGEX)) {
                errors.add(new ValidationError(rowCount, "marca", "Il campo marca non rispetta la regex: " + MARCA_REGEX));
            }
            if (!csvRecord.get("modello").matches(MODELLO_REGEX)) {
                errors.add(new ValidationError(rowCount, "modello", "Il campo modello non rispetta la regex: " + MODELLO_REGEX));
            }
            if (!csvRecord.get("codiceProdotto").matches(CODICE_PRODOTTO_REGEX)) {
                errors.add(new ValidationError(rowCount, "codiceProdotto", "Il campo codiceProdotto non rispetta la regex: " + CODICE_PRODOTTO_REGEX));
            }
        return errors;
    }


  public void downloadReport(String idUpload) {
    //FIND A DB DEL DOCUMNET LEGATO ALL UPLOAD
    //RICERCA SULLO STORAGE IN BASE AI DATI RECUPERATO
  }
}


