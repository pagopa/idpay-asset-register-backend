package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.mapper.product.ProductMapperStrategy;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.CategoryExternalCheck;
import it.gov.pagopa.register.model.initiative.ExternalCheckTemplate;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.external.system.check.ExternalCheckExecutor;
import it.gov.pagopa.register.service.validator.external.system.check.ExternalCheckResult;
import it.gov.pagopa.register.service.validator.product.ValidationService;
import it.gov.pagopa.register.utils.CsvUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.ERROR_MAP;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.ErrorKey.DUPLICATE_GTIN_EAN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceDuplicateTest {

  private static final String GTIN_HEADER = "Codice GTIN/EAN";
  private static final String CODE_HEADER = "Codice Prodotto";
  private static final List<String> HEADERS = List.of(GTIN_HEADER, CODE_HEADER);

  @Mock
  private ProductRepository productRepository;
  @Mock
  private ProductMapperStrategy mapper;
  @Mock
  private ExternalCheckExecutor externalCheckExecutor;

  private ValidationService validationService;
  private CategoryConfig category;

  @BeforeEach
  void setUp() {
    validationService = new ValidationService(productRepository, Map.of("TEST", mapper), externalCheckExecutor);
    category = mock(CategoryConfig.class);
    when(category.getProductMapper()).thenReturn("TEST");
    when(mapper.extractBusinessKey(any(), eq(category)))
      .thenAnswer(call -> ((CSVRecord) call.getArgument(0)).get(GTIN_HEADER));
    when(productRepository.findByGtinCodeAndInitiativeId(any(), eq("initiative")))
      .thenReturn(Optional.empty());
    when(mapper.mapToProduct(any(), any(), any(), any(), any(), any(), any()))
      .thenAnswer(call -> {
        CSVRecord row = call.getArgument(0);
        Product product = new Product();
        product.setGtinCode(row.get(GTIN_HEADER));
        product.setProductCode(row.get(CODE_HEADER));
        return product;
      });
  }

  @ParameterizedTest(name = "{0} occurrences: keep the first, report every later row")
  @ValueSource(ints = {2, 3})
  void shouldKeepFirstOccurrenceAndReportLaterDuplicates(int occurrences, @TempDir Path directory)
    throws IOException {
    String csv = "GTIN1;FIRST\nGTIN2;CONTROL\nGTIN1;SECOND\n";
    if (occurrences == 3) {
      csv += "GTIN1;THIRD\n";
    }
    List<CSVRecord> records = readRecords(csv);
    when(category.getExternalChecks()).thenReturn(List.of());

    ProductValidationResult result = validate(records, mock(InitiativeConfig.class));

    assertEquals(Set.of("GTIN1", "GTIN2"), result.getValidRecords().keySet());
    assertEquals("FIRST", result.getValidRecords().get("GTIN1").getProductCode());
    assertEquals("CONTROL", result.getValidRecords().get("GTIN2").getProductCode());
    List<CSVRecord> duplicates = records.subList(2, records.size());
    assertEquals(duplicates, result.getInvalidRecords());
    assertEquals(occurrences - 1, result.getErrorMessages().size());
    for (CSVRecord duplicate : duplicates) {
      assertEquals(ERROR_MAP.get(DUPLICATE_GTIN_EAN), result.getErrorMessages().get(duplicate));
      verify(mapper, never()).mapToProduct(eq(duplicate), any(), any(), any(), any(), any(), any());
    }
    assertFalse(result.getErrorMessages().containsKey(records.getFirst()));

    Path report = directory.resolve("duplicates.csv");
    CsvUtils.writeCsvWithErrors(result.getInvalidRecords(), HEADERS, result.getErrorMessages(), report);
    String reportText = Files.readString(report, StandardCharsets.UTF_8);
    try (CSVParser parser = csvFormat().parse(new StringReader(reportText.replaceFirst("^\uFEFF", "")))) {
      List<CSVRecord> reportRows = parser.getRecords();
      assertEquals(occurrences - 1, reportRows.size());
      for (int i = 0; i < reportRows.size(); i++) {
        assertEquals("GTIN1", reportRows.get(i).get(GTIN_HEADER));
        assertEquals(duplicates.get(i).get(CODE_HEADER), reportRows.get(i).get(CODE_HEADER));
        assertEquals(ERROR_MAP.get(DUPLICATE_GTIN_EAN), reportRows.get(i).get("Errori di validazione"));
      }
    }
  }

  @Test
  void shouldKeepFirstValidOccurrenceWhenEarlierOccurrenceFailsExternalChecks() throws IOException {
    List<CSVRecord> records = readRecords("GTIN1;INVALID\nGTIN1;FIRSTVALID\nGTIN1;DUPLICATE\n");
    CategoryExternalCheck check = mock(CategoryExternalCheck.class);
    when(check.getKey()).thenReturn("CHECK");
    when(check.getParameters()).thenReturn(Map.of());
    when(category.getExternalChecks()).thenReturn(List.of(check));
    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);
    InitiativeConfig initiative = mock(InitiativeConfig.class);
    when(initiative.getExternalCheckTemplates()).thenReturn(Map.of("CHECK", template));
    when(externalCheckExecutor.execute(any(), any(), any(), any(), any()))
      .thenReturn(ExternalCheckResult.ko("EXTERNAL_ERROR"), ExternalCheckResult.ok(Map.of()));

    ProductValidationResult result = validate(records, initiative);

    assertEquals(Set.of("GTIN1"), result.getValidRecords().keySet());
    assertEquals("FIRSTVALID", result.getValidRecords().get("GTIN1").getProductCode());
    assertEquals(List.of(records.get(0), records.get(2)), result.getInvalidRecords());
    assertEquals(Map.of(records.get(0), "EXTERNAL_ERROR",
                       records.get(2), ERROR_MAP.get(DUPLICATE_GTIN_EAN)), result.getErrorMessages());
    verify(externalCheckExecutor, times(2)).execute(any(), any(), any(), any(), any());
    verify(externalCheckExecutor, never()).execute(eq(records.get(2)), any(), any(), any(), any());
    verify(mapper, times(1)).mapToProduct(any(), any(), any(), any(), any(), any(), any());
    verify(mapper).mapToProduct(eq(records.get(1)), any(), any(), any(), any(), any(), any());
  }

  private ProductValidationResult validate(List<CSVRecord> records, InitiativeConfig initiative) {
    return validationService.validateRecords(records, "category", "organization", "initiative", "file", "Organization", initiative, category, List.of("UPLOADED"));
  }

  private static List<CSVRecord> readRecords(String rows) throws IOException {
    try (CSVParser parser = csvFormat().parse(new StringReader(String.join(";", HEADERS) + "\n" + rows))) {
      return parser.getRecords();
    }
  }

  private static CSVFormat csvFormat() {
    return CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true).build();
  }
}
