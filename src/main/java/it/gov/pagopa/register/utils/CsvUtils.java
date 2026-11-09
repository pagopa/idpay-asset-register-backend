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
import java.util.*;

public class CsvUtils {

  private CsvUtils(){}

  public static final String DELIMITER = ";";

  private static String readAndSanitizeContent(MultipartFile file) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder();
      char[] buffer = new char[8192];
      int charsRead;
      boolean firstCharsChecked = false;

      while ((charsRead = reader.read(buffer)) != -1) {
        String chunk = new String(buffer, 0, charsRead);
        if (!firstCharsChecked) {
          if (chunk.startsWith("\uFEFF")) {
            chunk = chunk.substring(1);
          }
          firstCharsChecked = true;
        }
        sb.append(chunk);
      }
      return sb.toString();
    }
  }

  public static List<CSVRecord> readCsvRecords(MultipartFile file) throws IOException {
    String content = readAndSanitizeContent(file);
    try (Reader reader = new StringReader(content);
         CSVParser parser = CSVFormat.Builder.create()
           .setTrim(Boolean.TRUE)
           .setHeader().setSkipHeaderRecord(Boolean.TRUE)
           .setDelimiter(DELIMITER)
           .build()
           .parse(reader)) {
      return parser.getRecords();
    }
  }

  public static List<String> readHeaders(MultipartFile file) throws IOException {
    String content = readAndSanitizeContent(file);
    try (Reader reader = new StringReader(content);
         CSVParser parser = CSVFormat.Builder.create()
           .setHeader()
           .setTrim(true)
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
    finalHeaders.add("Errori di validazione");

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
