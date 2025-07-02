package it.gov.pagopa.register.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvUtils {

  public static List<CSVRecord> readCsvRecords(MultipartFile file) throws IOException {
    Reader reader = new InputStreamReader(file.getInputStream());
    CSVParser parser = CSVFormat.DEFAULT
      .withFirstRecordAsHeader()
      .withTrim()
      .parse(reader);
    return parser.getRecords();
  }

  public static List<String> readHeader(MultipartFile file) throws IOException {
    try (Reader reader = new InputStreamReader(file.getInputStream());
         CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
      return new ArrayList<>(parser.getHeaderMap().keySet());
    }
  }

  public static void writeCsvWithErrors(List<CSVRecord> invalidRecords, List<String> headers, Map<CSVRecord, String> errorMap, String filename) throws IOException {
    List<String> finalHeaders = new ArrayList<>(headers);
    finalHeaders.add("ValidationErrors");

    File output = new File("/tmp/" + filename);
    try (BufferedWriter writer = Files.newBufferedWriter(output.toPath());
         CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(finalHeaders.toArray(new String[0])))) {

      for (CSVRecord record : invalidRecords) {
        List<String> row = new ArrayList<>();
        for (String h : headers) {
          row.add(record.get(h));
        }
        row.add(errorMap.get(record)); // Messaggi concatenati
        printer.printRecord(row);
      }
    }
  }

}
