package it.gov.pagopa.register.model.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

//@Data
@Builder
@Document("asset_file")
//@NoArgsConstructor
//@AllArgsConstructor
public class UploadCsv {

  //userId
  //orgName
  //idUpload
  //fileName
  //time
  //uploadStatus
}
