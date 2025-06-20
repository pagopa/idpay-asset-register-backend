package it.gov.pagopa.register.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@Document("report_error_csv")
@NoArgsConstructor
@AllArgsConstructor
public class ReportError {
  private String productId;
  private List<ValidationError> errors;
}
