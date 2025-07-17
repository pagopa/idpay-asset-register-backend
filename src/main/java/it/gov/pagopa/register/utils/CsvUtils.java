package it.gov.pagopa.register.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvUtils {

  private CsvUtils(){}

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

  public static List<CSVRecord> readCsvRecords(ByteArrayOutputStream file) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
      new ByteArrayInputStream(file.toByteArray()), StandardCharsets.UTF_8));
         CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create()
           .setHeader()
           .setTrim(true)
           .setDelimiter(';')
           .build())) {
      return parser.getRecords();
    }
  }

  public static List<String> readHeader(ByteArrayOutputStream file) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
      new ByteArrayInputStream(file.toByteArray()), StandardCharsets.UTF_8));
         CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create()
           .setHeader()
           .setTrim(true)
           .setDelimiter(';')
           .build())) {
      return parser.getHeaderNames();
    }
  }

  public static void writeCsvWithErrors(List<CSVRecord> invalidRecords, List<String> headers, Map<CSVRecord, String> errorMap, Path outputPath) throws IOException {
    List<String> finalHeaders = new ArrayList<>(headers);
    finalHeaders.add("Validation Errors");

    try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
      writer.write("\uFEFF"); // BOM
      try (CSVPrinter printer = new CSVPrinter(writer,
        CSVFormat.Builder.create()
          .setHeader(finalHeaders.toArray(new String[0]))
          .setTrim(true)
          .setDelimiter(DELIMITER)
          .build())) {

        for (CSVRecord csvRecord : invalidRecords) {
          List<String> row = new ArrayList<>();
          for (String h : headers) {
            row.add(csvRecord.get(h));
          }
          row.add(errorMap.get(csvRecord));
          printer.printRecord(row);
        }
      }
    }
  }


}
