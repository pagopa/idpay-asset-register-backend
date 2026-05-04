package it.gov.pagopa.register.service.validator.rule;

import lombok.Data;
import org.apache.commons.csv.CSVRecord;

public class CsvRuleContext implements RuleContext {

  private final CSVRecord record;
  private final String category;

  public CsvRuleContext(CSVRecord record, String category) {
    this.record = record;
    this.category = category;
  }

  public String getValue(String field) {
    return record.get(field);
  }

}
