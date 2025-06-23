package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.exception.operation.CsvValidationException;
import it.gov.pagopa.register.exception.role.PermissionNotFoundException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@NoArgsConstructor
public class AuthorizationService {

  public void validateAction(String role, String operation){
    if(false)
      throw new PermissionNotFoundException("il seguente ruolo non puo eseguire questa operazione");
  }
}
