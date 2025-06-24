package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.enums.UploadCsvStatus;
import it.gov.pagopa.register.exception.operation.CsvValidationException;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.UploadCsv;
import it.gov.pagopa.register.repository.operation.UploadRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static it.gov.pagopa.register.utils.Utils.*;


@Service
@Slf4j
public class ProductService {

  private final UploadRepository uploadRepository;
  private final FileStorageClient azureBlobClient;

  public ProductService(UploadRepository uploadRepository, FileStorageClient azureBlobClient) {
    this.uploadRepository = uploadRepository;
    this.azureBlobClient = azureBlobClient;
  }

  @Value("${config.max-rows}")
  private int maxRows = 100;

  public void saveCsv(MultipartFile csv, String category, String idOrg, String idUser, String role) {
    if (Boolean.FALSE.equals(isCsv(csv)))
      throw new CsvValidationException("Il file inserito non è un .csv");
    log.info("Il file inserito è un .csv");

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv.getInputStream()));
         CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create().setHeader().setTrim(true).setDelimiter(';').build())) {

      Boolean isCookinghobs = COOKINGHOBS.equals(category);
      Set<String> headers = new HashSet<>(csvParser.getHeaderNames());
      Set<String> csvHeader = Boolean.TRUE.equals(isCookinghobs) ? CSV_HEADER_PIANI_COTTURA : CSV_HEADER_PRODOTTI;

      if (!headers.equals(csvHeader)) {
        throw new CsvValidationException("header csv non validi");
      }
      log.info("header csv validi");
      List<CSVRecord> records = csvParser.getRecords();
      if (records.size() > maxRows + 1)
        throw new CsvValidationException("numero di record nel csv maggiore di " + maxRows);
      log.info("numero di record nel csv valide");
      String idUpload = idOrg + "-" + category + "-" + idUser + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
      log.info("idUpload {}", idUpload);

      Map<String, String> rowWithErrors = new LinkedHashMap<>();
      for (CSVRecord csvRecord : records) {
        List<String> errors;

        if (Boolean.TRUE.equals(isCookinghobs)) {
          errors = checkPianiCotturaCsvRow(csvRecord);
        } else {
          errors = checkProdottiCsvRow(csvRecord, category);
        }

        if (!errors.isEmpty()) {
          for (String header : csvHeader) {
            rowWithErrors.put(header, csvRecord.get(header));
          }
          rowWithErrors.put("Errori", String.join(", ", errors));
        }
      }

      String fileName = csv.getOriginalFilename();

      UploadCsv uploadFile = new UploadCsv(
        idUser,
        idOrg,
        idUpload,
        LocalDateTime.now(),
        UploadCsvStatus.LOADING_CHECK.toString(),
        null,
        null,
        fileName);
      log.info("uploadFile: {}", uploadFile);

      if (rowWithErrors.isEmpty()) {
        uploadFile.setStatus(UploadCsvStatus.FORMAL_OK.toString());
        uploadRepository.save(uploadFile);

        String destination = "CSV/" + idUpload + ".csv";
        log.info("destination: {}", destination);
        azureBlobClient.upload(csv.getResource().getInputStream(),
          destination,
          csv.getContentType());

      } else {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(idUpload + ".csv"));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.Builder.create().setHeader(csvHeader.toArray(new String[0])).setTrim(true).build())) {
          csvPrinter.printRecord(rowWithErrors.values());

          uploadFile.setStatus(UploadCsvStatus.FORMAL_KO.toString());
          uploadRepository.save(uploadFile);

          String destination = "Report/Eprel_Error/";

          azureBlobClient.uploadFile(null,
            destination,
            csv.getContentType());
        } catch (IOException e) {
          throw new CsvValidationException("Errore nella scrittura del file CSV di report: " + e.getMessage());
        }
      }
    } catch (IOException e) {
      throw new CsvValidationException("Errore nella lettura del file CSV: " + e.getMessage());
    }
  }

  private Boolean isCsv(MultipartFile file) {
    return file != null && "text/csv".equalsIgnoreCase(file.getContentType());
  }

  private List<String> checkPianiCotturaCsvRow(CSVRecord csvRecord) {
    List<String> errors = new ArrayList<>();
    if (!csvRecord.get(CODICE_GTIN_EAN).matches(CODICE_GTIN_EAN_REGEX)) {
      errors.add(ERROR_GTIN_EAN);
    }
    if (!COOKINGHOBS.contains(csvRecord.get(CATEGORIA))) {
      errors.add(ERROR_CATEGORIA_COOKINGHOBS);
    }
    if (!csvRecord.get(MARCA).matches(MARCA_REGEX)) {
      errors.add(ERROR_MARCA);
    }
    if (!csvRecord.get(MODELLO).matches(MODELLO_REGEX)) {
      errors.add(ERROR_MODELLO);
    }
    if (!csvRecord.get(CODICE_PRODOTTO).matches(CODICE_PRODOTTO_REGEX)) {
      errors.add(ERROR_CODICE_PRODOTTO);
    }
    if (!csvRecord.get(PAESE_DI_PRODUZIONE).matches(PAESE_DI_PRODUZIONE_REGEX)) {
      errors.add(ERROR_PAESE_DI_PRODUZIONE);
    }
    return errors;
  }

  private List<String> checkProdottiCsvRow(CSVRecord csvRecord, String category) {
    List<String> errors = new ArrayList<>();
    if (!csvRecord.get(CODICE_EPREL).matches(CODICE_EPREL_REGEX)) {
      errors.add(ERROR_CODICE_EPREL);
    }
    if (!csvRecord.get(CODICE_GTIN_EAN).matches(CODICE_GTIN_EAN_REGEX)) {
      errors.add(ERROR_GTIN_EAN);
    }
    if (!CATEGORIE_PRODOTTI.contains(csvRecord.get(CATEGORIA))) {
      errors.add(ERROR_CATEGORIA_PRODOTTI + category);
    }
    if (!csvRecord.get(CODICE_PRODOTTO).matches(CODICE_PRODOTTO_REGEX)) {
      errors.add(ERROR_CODICE_PRODOTTO);
    }
    if (!csvRecord.get(PAESE_DI_PRODUZIONE).matches(PAESE_DI_PRODUZIONE_REGEX)) {
      errors.add(ERROR_PAESE_DI_PRODUZIONE);
    }
    return errors;
  }


  public ByteArrayOutputStream downloadReport(String idUpload) {
    UploadCsv upload = uploadRepository.findByIdUpload(idUpload)
      .orElseThrow(() -> new ReportNotFoundException("Report non trovato con id: " + idUpload));

    String filePath = "";

    if (upload.getStatus().equalsIgnoreCase("EPREL_ERROR")) {
      filePath = "Report/Eprel_Error/" + upload.getIdUpload() + ".csv";
    } else if (upload.getStatus().equalsIgnoreCase("FORMAL_ERROR")) {
      filePath = "Report/Formal_Error/" + upload.getIdUpload() + ".csv";
    } else {
      throw new ReportNotFoundException("Tipo di errore non supportato: " + upload.getStatus());
    }

    ByteArrayOutputStream result = azureBlobClient.download(filePath);
    if (result == null) {
      throw new ReportNotFoundException("Report non trovato su Azure per path: " + filePath);
    }
    return result;
  }
}

