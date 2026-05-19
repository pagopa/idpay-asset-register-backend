package it.gov.pagopa.register.service.validator.rule.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsvRuleContextTest {

  @Mock
  private CSVRecord csvRecordMock;

  @Test
  void testCsvRuleContextCreationAndGetters() {
    String expectedCategory = "Welfare";

    CsvRuleContext context = new CsvRuleContext(csvRecordMock, expectedCategory);

    assertNotNull(context);
    assertEquals(csvRecordMock, context.getCsvRecord());
    assertEquals(expectedCategory, context.getCategory());
  }

  @Test
  void testGetValue() {
    String headerName = "initiativeName";
    String expectedValue = "Bonus Elettrodomestici";
    String category = "Welfare";

    when(csvRecordMock.get(headerName)).thenReturn(expectedValue);

    CsvRuleContext context = new CsvRuleContext(csvRecordMock, category);

    String actualValue = context.getValue(headerName);

    assertEquals(expectedValue, actualValue);
  }
}
