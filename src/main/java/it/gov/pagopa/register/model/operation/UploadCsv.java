package it.gov.pagopa.register.model.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document("upload_csv")
@NoArgsConstructor
@AllArgsConstructor
public class UploadCsv {

  private String userId;
  private String orgName;
  private String idUpload;
  private LocalDateTime generationDate;
  private String category;

  // formalKo, formalOK, eprelKO, loadindCheck
  private String status;
}
