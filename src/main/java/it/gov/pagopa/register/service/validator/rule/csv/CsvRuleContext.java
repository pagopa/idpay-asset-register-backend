package it.gov.pagopa.register.service.validator.rule.csv;

import it.gov.pagopa.register.service.validator.rule.RuleContext;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.csv.CSVRecord;
@Data
public class CsvRuleContext implements RuleContext {

  private final CSVRecord csvRecord;

  private final String category;

  public String getValue(String header) {
    return csvRecord.get(header);
  }

}
