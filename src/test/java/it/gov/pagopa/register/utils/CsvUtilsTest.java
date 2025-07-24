package it.gov.pagopa.register.utils;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilsTest {

  private static final String CSV_CONTENT = "Name;Age\nJohn;30\nJane;25\n";
  private static final String CSV_INVALID_CONTENT = "Name;Age\nJohn;abc\nJane;25\n";

  @Test
  void testReadCsvRecordsFromMultipartFile() throws IOException {
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", CSV_CONTENT.getBytes());
    List<CSVRecord> records = CsvUtils.readCsvRecords(file);
    assertEquals(2, records.size());
    assertEquals("John", records.get(0).get("Name"));
  }

  @Test
  void testReadHeaderFromMultipartFile() throws IOException {
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", CSV_CONTENT.getBytes());
    List<String> headers = CsvUtils.readHeader(file);
    assertEquals(List.of("Name", "Age"), headers);
  }

  @Test
  void testReadCsvRecordsFromByteArrayOutputStream() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(CSV_CONTENT.getBytes());
    List<CSVRecord> records = CsvUtils.readCsvRecords(baos);
    assertEquals(2, records.size());
    assertEquals("Jane", records.get(1).get("Name"));
  }

  @Test
  void testReadHeaderFromByteArrayOutputStream() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(CSV_CONTENT.getBytes());
    List<String> headers = CsvUtils.readHeader(baos);
    assertEquals(List.of("Name", "Age"), headers);
  }

  @Test
  void testWriteCsvWithErrors() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(CSV_INVALID_CONTENT.getBytes());
    List<CSVRecord> records = CsvUtils.readCsvRecords(baos);
    List<String> headers = CsvUtils.readHeader(baos);

    Map<CSVRecord, String> errorMap = new HashMap<>();
    errorMap.put(records.get(0), "Invalid age format");

    Path tempFile = Files.createTempFile("invalid", ".csv");
    CsvUtils.writeCsvWithErrors(List.of(records.get(0)), headers, errorMap, tempFile);

    List<String> lines = Files.readAllLines(tempFile);
    assertTrue(lines.get(0).contains("Errori di validazione"));
    assertTrue(lines.get(1).contains("Invalid age format"));

    Files.deleteIfExists(tempFile);
  }
}
