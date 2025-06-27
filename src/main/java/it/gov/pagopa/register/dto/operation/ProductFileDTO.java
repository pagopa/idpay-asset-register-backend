package it.gov.pagopa.register.dto.operation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductFileDTO {
  private String idUser;
  private String idUpload;
  private LocalDateTime uploadDate;
  private String status;
  private Integer totalUpload;
  private Integer failedUpload;
  private String originalFileName;
}
