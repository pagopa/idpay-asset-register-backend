package it.gov.pagopa.register.service.rolepermission;

import it.gov.pagopa.register.dto.rolepermission.UserPermissionDTO;
import org.springframework.stereotype.Service;

@Service
public interface RolePermissionService {

    UserPermissionDTO getUserPermission(String institution);

}
