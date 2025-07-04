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

  public static final String DELIMITER = ";";

  public static List<CSVRecord> readCsvRecords(MultipartFile file) throws IOException {
    Reader reader = new InputStreamReader(file.getInputStream());
    CSVParser parser = CSVFormat.Builder.create()
      .setTrim(Boolean.TRUE)
      .setHeader().setSkipHeaderRecord(Boolean.TRUE)
      .setDelimiter(DELIMITER)
      .build()
      .parse(reader);
    return parser.getRecords();
  }

  public static List<String> readHeader(MultipartFile file) throws IOException {
    try (Reader reader = new InputStreamReader(file.getInputStream());
         CSVParser parser = CSVFormat.Builder.create()
           .setTrim(Boolean.TRUE)
           .setHeader().setSkipHeaderRecord(Boolean.TRUE)
           .setDelimiter(DELIMITER)
           .build()
           .parse(reader)) {
      return parser.getHeaderNames();
    }
  }

  public static void writeCsvWithErrors(List<CSVRecord> invalidRecords, List<String> headers, Map<CSVRecord, String> errorMap, String filename) throws IOException {
    List<String> finalHeaders = new ArrayList<>(headers);
    finalHeaders.add("Validation Errors");

    File output = new File("/tmp/" + filename);
    try (BufferedWriter writer = Files.newBufferedWriter(output.toPath());
         CSVPrinter printer = new CSVPrinter(writer, CSVFormat.Builder.create().setHeader(finalHeaders.toArray(new String[0])).setTrim(Boolean.TRUE).setDelimiter(DELIMITER).build())) {

      for (CSVRecord record : invalidRecords) {
        List<String> row = new ArrayList<>();
        for (String h : headers) {
          row.add(record.get(h));
        }
        row.add(errorMap.get(record));
        printer.printRecord(row);
      }
    }
  }

}
