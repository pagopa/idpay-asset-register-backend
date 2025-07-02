package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.UploadKeyConstant;
import it.gov.pagopa.register.constants.enums.UploadCsvStatus;
import it.gov.pagopa.register.dto.mapper.operation.AssetProductDTO;
import it.gov.pagopa.register.exception.operation.CsvValidationException;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.UploadCsv;
import it.gov.pagopa.register.repository.operation.UploadRepository;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static it.gov.pagopa.register.utils.Utils.*;


@Service
@Slf4j
public class ProductServiceOld {

  private final UploadRepository uploadRepository;
  private final FileStorageClient azureBlobClient;
  private final ProductFileValidator productFileValidator;

  public ProductService(UploadRepository uploadRepository, FileStorageClient azureBlobClient,ProductFileValidator productFileValidator) {
    this.uploadRepository = uploadRepository;
    this.azureBlobClient = azureBlobClient;
    this.productFileValidator = productFileValidator;
  }

  @Value("${config.max-rows}")
  int maxRows = 100;


  public AssetProductDTO processFile(MultipartFile file, String category, String idOrg, String idUser) {

    try {
      String originalFileName = file.getOriginalFilename();
      String batchId = UUID.randomUUID().toString();
      Instant now = Instant.now();

      // 0. Validazione preliminare: file vuoto
      if (file == null || file.isEmpty() || file.getSize() == 0) {
        return FileProcessingResult.ko("EMPTY_FILE");
      }

      // 0.1. Verifica estensione e tipo mime (grezza ma utile)
      if (!originalFileName.endsWith(".csv")) {
        return FileProcessingResult.ko("INVALID_FILE_TYPE");
      }

      // 1. Carica configurazione
      LinkedHashMap<String, ColumnDefinition> columnDefinitions = columnConfigService.getColumnDefinitions(category);
      if (columnDefinitions == null || columnDefinitions.isEmpty()) {
        return FileProcessingResult.ko("UNKNOWN_CATEGORY");
      }

      // 2. Legge header
      List<String> actualHeader = CsvUtils.readHeader(file);
      List<String> expectedHeader = new ArrayList<>(columnDefinitions.keySet());

      if (!actualHeader.equals(expectedHeader)) {
        return FileProcessingResult.ko("INVALID_HEADER");
      }

      // 3. Legge record (dopo aver letto l'header)
      List<CSVRecord> records = CsvUtils.readCsvRecords(file);
      int totalRecords = records.size();

      if (totalRecords == 0) {
        return FileProcessingResult.ko("NO_DATA");
      }

      if (totalRecords > MAX_RECORDS) {
        return FileProcessingResult.ko("TOO_MANY_RECORDS");
      }

      // 4. Validazione dei singoli record
      List<CSVRecord> invalidRecords = new ArrayList<>();
      Map<CSVRecord, String> errorMessages = new HashMap<>();

      for (CSVRecord record : records) {
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, ColumnDefinition> entry : columnDefinitions.entrySet()) {
          String columnName = entry.getKey();
          ColumnDefinition def = entry.getValue();
          String value = record.get(columnName);

          if (value == null || !def.getPattern().matcher(value).matches()) {
            errors.add(def.getErrorMessage());
          }
        }

        if (!errors.isEmpty()) {
          invalidRecords.add(record);
          errorMessages.put(record, String.join(" | ", errors));
        }
      }

      // 5. Se ci sono errori, salva file scarti
      int invalidCount = invalidRecords.size();
      if (invalidCount > 0) {
        String scartiFilename = "scarti_" + originalFileName;
        CsvUtils.writeCsvWithErrors(invalidRecords, expectedHeader, errorMessages, scartiFilename);

        processingLogRepository.save(new ProcessingLog(
          batchId, originalFileName, category, totalRecords, invalidCount,
          "FORMAL_ERROR", now
        ));

        return FileProcessingResult.ko("FORMAL_ERRORS_FOUND");
      }

      // 6. Upload su Azure
      blobStorageService.upload(file, batchId);

      // 7. Log OK
      processingLogRepository.save(new ProcessingLog(
        batchId, originalFileName, category, totalRecords, 0,
        "LOADED", now
      ));

      return FileProcessingResult.ok(batchId);

    } catch (Exception e) {
      e.printStackTrace();
      return FileProcessingResult.ko("GENERIC_ERROR");
    }


  }

  public AssetProductDTO saveCsv(MultipartFile csv, String category, String idOrg, String idUser) {
    if (Boolean.FALSE.equals(isCsv(csv))) {
      return new AssetProductDTO(
        UploadCsvStatus.FORMAL_ERROR.toString(),
        UploadKeyConstant.EXTENSION_FILE_ERROR.getKey(),
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
    }
    log.info("Il file inserito è un .csv");

    String idUpload = null;
    Map<String, String> rowWithErrors = new LinkedHashMap<>();
    Set<String> csvHeader = new HashSet<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8));
         CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create().setHeader().setTrim(true).setDelimiter(';').build())) {

      Boolean isCookinghobs = COOKINGHOBS.equals(category);
      Set<String> headers = new HashSet<>(csvParser.getHeaderNames());
      csvHeader = Boolean.TRUE.equals(isCookinghobs) ? CSV_HEADER_PIANI_COTTURA : CSV_HEADER_PRODOTTI;

      if (!headers.equals(csvHeader)) {
        return new AssetProductDTO(
          UploadCsvStatus.FORMAL_ERROR.toString(),
          UploadKeyConstant.HEADER_FILE_ERROR.getKey(),
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
      }
      log.info("header csv validi");
      List<CSVRecord> records = csvParser.getRecords();
      if (records.size() > maxRows + 1)
        return new AssetProductDTO(
          UploadCsvStatus.FORMAL_ERROR.toString(),
          UploadKeyConstant.MAX_ROW_FILE_ERROR.getKey(),
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
      log.info("numero di record nel csv valide");
      idUpload = idOrg + "-" + category + "-" + idUser + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
      log.info("idUpload {}", idUpload);


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
    } catch (IOException e) {
      throw new CsvValidationException("Errore nella lettura del file CSV: " + e.getMessage());
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
        try {
          azureBlobClient.upload(csv.getResource().getInputStream(),
            destination,
            csv.getContentType());
          return new AssetProductDTO(
            UploadCsvStatus.FORMAL_OK.toString(),
            UploadKeyConstant.UPLOAD_FILE_OK.getKey(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
        } catch (IOException e) {
          throw new CsvValidationException("Errore nel caricamento del file CSV su Azure:" + e.getMessage());
        }



      } else {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.Builder.create().setHeader(csvHeader.toArray(new String[0])).setTrim(true).setDelimiter(";").build())) {
              log.info(rowWithErrors.toString());
              csvPrinter.printRecord(rowWithErrors.values());
              csvPrinter.flush();
              writer.flush();
              ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

              uploadFile.setStatus(String.valueOf(UploadCsvStatus.FORMAL_ERROR));
              uploadRepository.save(uploadFile);

              String destination = "Report/Formal_Error/"+idUpload+".csv";

              azureBlobClient.upload(inputStream,
                destination,
                csv.getContentType());
          return new AssetProductDTO(
            UploadCsvStatus.FORMAL_ERROR.toString(),
            UploadKeyConstant.REPORT_FORMAL_FILE_ERROR.getKey(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
        } catch (IOException e) {
          throw new CsvValidationException("Errore nella scrittura del file CSV di report: " + e.getMessage());
        }
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
      Locale.getISOCountries().
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

