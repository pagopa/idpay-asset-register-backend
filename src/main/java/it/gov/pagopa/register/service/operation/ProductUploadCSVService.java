package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.UploadKeyConstant;
import it.gov.pagopa.register.constants.enums.UploadCsvStatus;
import it.gov.pagopa.register.dto.operation.AssetProductDTO;
import it.gov.pagopa.register.exception.operation.CsvValidationException;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static it.gov.pagopa.register.constants.AssetRegisterConstant.*;


@Service
@Slf4j
public class ProductUploadCSVService {

  private final ProductFileRepository productFileRepository;
  private final FileStorageClient azureBlobClient;

  public ProductUploadCSVService(ProductFileRepository productFileRepository, FileStorageClient azureBlobClient) {
    this.productFileRepository = productFileRepository;
    this.azureBlobClient = azureBlobClient;
  }

  @Value("${config.max-rows}")
  int maxRows = 100;

  public AssetProductDTO saveCsv(MultipartFile csv, String category, String idOrg, String idUser) {
    if (Boolean.FALSE.equals(isCsv(csv)))
      return new AssetProductDTO(
        null,
        UploadCsvStatus.FORMAL_ERROR.toString(),
        UploadKeyConstant.EXTENSION_FILE_ERROR.getKey(),
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
    log.info("Il file inserito è un .csv");

    String idUpload = null;
    List<List<String>> rowWithErrors = new ArrayList<>();
    Set<String> csvHeader = new HashSet<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8));
         CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create().setHeader().setTrim(true).setDelimiter(';').build())) {

      Boolean isCookinghobs = COOKINGHOBS.equals(category);
      Set<String> headers = new HashSet<>(csvParser.getHeaderNames());
      csvHeader = Boolean.TRUE.equals(isCookinghobs) ? CSV_HEADER_PIANI_COTTURA : CSV_HEADER_PRODOTTI;

      if (!headers.equals(csvHeader)) {
        return new AssetProductDTO(
          null,
          UploadCsvStatus.FORMAL_ERROR.toString(),
          UploadKeyConstant.HEADER_FILE_ERROR.getKey(),
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
      }
      log.info("header csv validi");
      List<CSVRecord> records = csvParser.getRecords();
      if (records.size() > maxRows + 1)
        return new AssetProductDTO(
          null,
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
          List<String> row = new ArrayList<>(csvRecord.stream().toList());
          row.add(String.join(", ", errors));
          rowWithErrors.add(row);
        }
      }
    } catch (IOException e) {
      throw new CsvValidationException("Errore nella lettura del file CSV: " + e.getMessage());
    }

      String fileName = csv.getOriginalFilename();



      if (rowWithErrors.isEmpty()) {
        ProductFile productFile = new ProductFile(
          null,
          category,
          idUser,
          idOrg,
          fileName,
          UploadCsvStatus.FORMAL_OK.toString(),
          LocalDateTime.now(),
          null,
          null
        );
        log.info("uploadFile: {}", productFile);
        productFile = productFileRepository.save(productFile);
        String destination = "CSV/" + productFile.getId() + ".csv";
        log.info("destination: {}", destination);
        try {
          azureBlobClient.upload(csv.getResource().getInputStream(),
            destination,
            csv.getContentType());
          return new AssetProductDTO(
            productFile.getId(),
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
              csvPrinter.printRecord(rowWithErrors);
              csvPrinter.flush();
              writer.flush();
              ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

              ProductFile productFile = new ProductFile(
                null,
                category,
                idUser,
                idOrg,
                fileName,
                UploadCsvStatus.FORMAL_ERROR.toString(),
                LocalDateTime.now(),
                null,
                null
              );
              productFile = productFileRepository.save(productFile);

              String destination = "Report/Formal_Error/"+productFile.getId()+".csv";

              azureBlobClient.upload(inputStream,
                destination,
                csv.getContentType());
          return new AssetProductDTO(
            productFile.getId(),
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
    if (category.equals(csvRecord.get(CATEGORIA))) {
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
    ProductFile upload = productFileRepository.findById(idUpload)
      .orElseThrow(() -> new ReportNotFoundException("Report non trovato con id: " + idUpload));

    String filePath = "";

    if (upload.getUploadStatus().equalsIgnoreCase("EPREL_ERROR")) {
      filePath = "Report/Eprel_Error/" + upload.getId() + ".csv";
    } else if (upload.getUploadStatus().equalsIgnoreCase("FORMAL_ERROR")) {
      filePath = "Report/Formal_Error/" + upload.getId() + ".csv";
    } else {
      throw new ReportNotFoundException("Tipo di errore non supportato: " + upload.getUploadStatus());
    }

    ByteArrayOutputStream result = azureBlobClient.download(filePath);
    if (result == null) {
      throw new ReportNotFoundException("Report non trovato su Azure per path: " + filePath);
    }
    return result;
  }
}

