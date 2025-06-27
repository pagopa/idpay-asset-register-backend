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
public class ProductFile {
  private String idUser;
  private String idOrg;
  private String idUpload;
  private LocalDateTime uploadDate;
  private String status;
  private Integer totalUpload;
  private Integer failedUpload;
  private String originalFileName;
}
