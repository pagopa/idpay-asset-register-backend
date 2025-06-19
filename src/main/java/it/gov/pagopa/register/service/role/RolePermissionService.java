package it.gov.pagopa.register.service.role;

import it.gov.pagopa.register.dto.role.UserPermissionDTO;
import org.springframework.stereotype.Service;

@Service
public interface RolePermissionService {

    UserPermissionDTO getUserPermission(String role);

}
