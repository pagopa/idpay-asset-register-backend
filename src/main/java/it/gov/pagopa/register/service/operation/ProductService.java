package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.onetrust.InitiativeFileStorageClient;
import it.gov.pagopa.register.constants.enums.UploadCsvStatus;
import it.gov.pagopa.register.dto.operation.RegisterUploadReqeustDTO;
import it.gov.pagopa.register.exception.operation.CsvValidationException;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.UploadCsv;
import it.gov.pagopa.register.repository.operation.UploadRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

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




  public void saveCsv(MultipartFile csv, String category, String idOrg, String idUser, String role) {
  /*
    1. CONTROLLI CHE BLOCCO IL CONTROLLO DEL CONTENUTO

    1. Controllo estensione
    2. Controllo prima riga come header
    3. Controllo numero righe csvParser.getRecords().size();
   */

    //2. CONTROLLI DEL CONTENUTO

    //1.1

    //check estensione
    if (!isCsv(csv))
      throw new CsvValidationException("Il file inserito non è un .csv");

    // settere ; come separatore
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv.getInputStream()));
         CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create().setHeader().setTrim(true).build())){

      Boolean isPianoCottura = checkCategory(CATEGORIE_PIANI_COTTURA, category);
      //check headers
      if ((isPianoCottura && !new HashSet<>(CSV_HEADER_PIANI_COTTURA).containsAll(csvParser.getHeaderMap().keySet())) ||
          (!isPianoCottura && !new HashSet<>(CSV_HEADER_PRODOTTI).containsAll(csvParser.getHeaderMap().keySet())))
            throw new CsvValidationException("header csv non validi");
      List<String> csvHeader = new ArrayList<>(isPianoCottura ? CSV_HEADER_PIANI_COTTURA : CSV_HEADER_PRODOTTI);

      //check numero righe
      //da spostare nell'appProperties
      if (csvParser.getRecords().size() > MAX_ROWS + 1)
        throw new CsvValidationException("numero di record nel csv maggiore di " + MAX_ROWS);

      //GENERARE idUpload idOrg +category+ userId + timestamp
      String idUpload = idOrg + "-" + category + "-" + idUser + "-" + LocalDateTime.now();

      //Check contenuto
      Map<String, String> rowWithErrors = new LinkedHashMap<>();
      for (CSVRecord record : csvParser) {
        List<String> errors;
        String categoriaValue = record.get("Categoria");

        if ("COOKINGHOBS".equalsIgnoreCase(categoriaValue)) {
          errors = checkPianiCotturaCsvRow(record);
        } else {
          errors = checkProdottiCsvRow(record, category);
        }


        if (!errors.isEmpty()) {
          csvHeader.add("Errori");
          for (String header : csvHeader) {
            rowWithErrors.put(header, record.get(header));
          }
          rowWithErrors.put("Errori", String.join(", ", errors));
        }
      }


      String fileName = csv.getResource().getFile().getName();

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

      //se non ci sono stati errori
      if (rowWithErrors.isEmpty()) {

        // 1.1 Salvo a db i dati legati al caricamento è quelli necessari al successivo downlaod
        uploadFile.setStatus(UploadCsvStatus.FORMAL_OK.toString());
        uploadRepository.save(uploadFile);

        //1.2 Carico il file sullo storage di azure
        String destination = "CSV/" + idOrg + "/";
        azureBlobClient.uploadFile(csv.getResource().getFile(),
          destination,
          csv.getContentType());
                /*
                1.3 Restituo il seguento body e code
                String idUpload = ID GENERATO IN PRECEDENZA
                String statusUpload = QUALCOSA DEL TIPO PRESA IN CARICO;
                Status 200
               */
      }
      else {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(idUpload + ".csv"));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.Builder.create().setHeader(csvHeader.toArray(new String[0])).setTrim(true).build())) {
          csvPrinter.printRecord(rowWithErrors.values());

          // 2.1 Salvo a db i dati legati al caricamento è quelli necessari al successivo downlaod
          uploadFile.setStatus(UploadCsvStatus.FORMAL_KO.toString());
          uploadRepository.save(uploadFile);

          //2.2 Geniro il file di report e carico il file sullo storage di azure

          String destination = "Report/Eprel_Error/";
          // ReportError da cambiare in MultipartFile? (1 riga per prodotto)
          azureBlobClient.uploadFile(null,
            destination,
            csv.getContentType());

             /*
                2.3 Restituo il seguento body e code
                String idUpload = ID GENERATO IN PRECEDENZA
                String statusUpload = VALIDATION_ERROR;
                Status 500
              */
        }
        catch (IOException e) {
          throw new RuntimeException("Errore nella scrittura del file CSV di report: " + e.getMessage());
        }
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Errore nella lettura del file CSV: " + e.getMessage());
    }
  }

  private Boolean checkCategory(List<String> listaCategorie, String categoria){
    return listaCategorie.contains(categoria);
  }

  private Boolean isCsv(MultipartFile file){
      return file != null && !file.isEmpty() && file.getOriginalFilename() != null && !file.getOriginalFilename().toLowerCase().endsWith(".csv");
  }

  private List<String> checkPianiCotturaCsvRow(CSVRecord csvRecord) {
          List<String> errors = new ArrayList<>();
          if (!csvRecord.get("Codice GTIN/EAN").matches(CODICE_GTIN_EAN_REGEX)) {
              errors.add( "Il Codice GTIN/EAN è obbligatorio e deve essere univoco ed alfanumerico e lungo al massimo 14 caratteri");
          }
          if (!CATEGORIE_PIANI_COTTURA.contains(csvRecord.get("Categoria"))) {
              errors.add("Il campo Categoria è obbligatorio e deve contenere il valore fisso 'COOKINGHOBS'");
          }
          if (!csvRecord.get("Marca").matches(MARCA_REGEX)) {
              errors.add("Il campo Marca è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri");
          }
          if (!csvRecord.get("Modello").matches(MODELLO_REGEX)) {
              errors.add("Il campo Modello è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri");
          }
          if (!csvRecord.get("Codice prodotto").matches(CODICE_PRODOTTO_REGEX)) {
              errors.add( "Il Codice prodotto non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri");
          }
          if (!csvRecord.get("Paese di Produzione").matches(PAESE_DI_PRODUZIONE_REGEX)) {
            errors.add( "Il Paese di Produzione è obbligatorio e deve essere composto da esattamente 2 caratteri");
          }
      return errors;
  }

  private List<String> checkProdottiCsvRow(CSVRecord csvRecord, String category) {
    List<String> errors = new ArrayList<>();
    if (!csvRecord.get("Codice EPREL").matches(CODICE_EPREL_REGEX)) {
      errors.add("Il Codice EPREL è obbligatorio e deve essere un valore numerico");
    }
    if (!csvRecord.get("Codice GTIN/EAN").matches(CODICE_GTIN_EAN_REGEX)) {
      errors.add("Il Codice GTIN/EAN è obbligatorio e deve essere univoc ed alfanumerico e lungo al massimo 14 caratteri");
    }
    if (!CATEGORIE_PRODOTTI.contains(csvRecord.get("Categoria"))) {
      errors.add("Il campo Categoria è obbligatorio e deve contenere il valore fisso "+category);
    }
    if (!csvRecord.get("Codice prodotto").matches(CODICE_PRODOTTO_REGEX)) {
      errors.add("Il Codice prodotto non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri");
    }
    if (!csvRecord.get("Paese di Produzione").matches(PAESE_DI_PRODUZIONE_REGEX)) {
      errors.add("Il Paese di Produzione è obbligatorio e deve essere composto da esattamente 2 caratteri");
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
    if (upload.getStatus().equalsIgnoreCase("EPREL_ERROR")) {
      filePath = "Report/Eprel_Error/" + upload.getIdUpload() + ".csv";
    } else if (upload.getStatus().equalsIgnoreCase("FORMAL_ERROR")) {
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


