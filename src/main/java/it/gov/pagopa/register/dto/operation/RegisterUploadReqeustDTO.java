package it.gov.pagopa.register.dto.operation;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class RegisterUploadReqeustDTO {

  private String category;
  private MultipartFile csv;

}
