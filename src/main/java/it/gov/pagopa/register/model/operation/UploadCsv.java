package it.gov.pagopa.register.model.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@Document("upload_csv")
@NoArgsConstructor
@AllArgsConstructor
public class UploadCsv {

  Long userId;
  String orgName;
  Long idUpload;
  LocalDateTime generationDate;
  String category;
  String status;
}
