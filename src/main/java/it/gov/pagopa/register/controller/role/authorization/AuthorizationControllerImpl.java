package it.gov.pagopa.register.controller.role.authorization;

import it.gov.pagopa.register.dto.role.UserPermissionDTO;
import it.gov.pagopa.register.service.role.RolePermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorizationControllerImpl implements AuthorizationController {

    private final RolePermissionService rolePermissionService;

    public AuthorizationControllerImpl(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @Override
    public ResponseEntity<UserPermissionDTO> getUserPermissions(String role) {
        UserPermissionDTO userPermissionDTO = rolePermissionService.getUserPermission(role);
        return new ResponseEntity<>(userPermissionDTO, HttpStatus.OK);
    }

}
