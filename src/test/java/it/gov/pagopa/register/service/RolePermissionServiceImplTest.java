package it.gov.pagopa.register.service;


import it.gov.pagopa.register.constants.RoleConstants;
import it.gov.pagopa.register.dto.role.PermissionDTO;
import it.gov.pagopa.register.dto.role.UserPermissionDTO;
import it.gov.pagopa.register.exception.role.PermissionNotFoundException;
import it.gov.pagopa.register.model.role.Permission;
import it.gov.pagopa.register.model.role.RolePermission;
import it.gov.pagopa.register.repository.role.RolePermissionRepository;
import it.gov.pagopa.register.service.role.RolePermissionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = {RolePermissionService.class})
@Slf4j
class RolePermissionServiceImplTest {

    @Autowired
    RolePermissionService rolePermissionService;

    @MockBean
    RolePermissionRepository rolePermissionRepository;

    @Test
    void getUserPermissionReturnPermission() {
      UserPermissionDTO userPermissionDTO = new UserPermissionDTO();
      List<PermissionDTO> permissionDTOList = getPermissionDTOS();
      userPermissionDTO.setRole("invitalia");
      userPermissionDTO.setPermissions(permissionDTOList);

      RolePermission rolePermission = getRolePermission();

      Mockito.when(rolePermissionRepository.findByRole("invitalia")).thenReturn(Optional.of(rolePermission));

        UserPermissionDTO admin = rolePermissionService.getUserPermission("invitalia");

        assertEquals(userPermissionDTO, admin);
    }

  private static List<PermissionDTO> getPermissionDTOS() {
    List<PermissionDTO> permissionDTOList = new ArrayList<>();
    PermissionDTO permissionDTO = new PermissionDTO();
    permissionDTO.setMode("WRITE");
    permissionDTO.setName("Permission1");
    permissionDTO.setDescription("Permission1");
    permissionDTOList.add(permissionDTO);
    PermissionDTO permissionDTO2 = new PermissionDTO();
    permissionDTO2.setMode("WRITE");
    permissionDTO2.setName("Permission2");
    permissionDTO2.setDescription("Permission2");
    permissionDTOList.add(permissionDTO2);
    return permissionDTOList;
  }

  private static RolePermission getRolePermission() {
    RolePermission rolePermission = new RolePermission();
    List<Permission> permissionList = new ArrayList<>();
    Permission permission = new Permission();
    permission.setMode("WRITE");
    permission.setName("Permission1");
    permission.setDescription("Permission1");
    permissionList.add(permission);
    Permission permission2 = new Permission();
    permission2.setMode("WRITE");
    permission2.setName("Permission2");
    permission2.setDescription("Permission2");
    permissionList.add(permission2);
    rolePermission.setRole("invitalia");
    rolePermission.setDescription("Administrator");
    rolePermission.setPermissions(permissionList);
    return rolePermission;
  }

  @Test
    void rolePermissionRepository_NotNull() {
        assertNotNull(rolePermissionRepository);
    }

    @Test
    void rolePermissionService_NotNull() {
        assertNotNull(rolePermissionService);
    }

    @Test
    void getInvitaliaRole_ok() {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole("invitalia");

        UserPermissionDTO userPermissionDTO = new UserPermissionDTO();
        userPermissionDTO.setRole("invitalia");
        userPermissionDTO.setPermissions(new ArrayList<>());

        Mockito.when(rolePermissionRepository.findByRole(anyString()))
                .thenReturn(Optional.of(rolePermission));
        UserPermissionDTO userPermission = rolePermissionService.getUserPermission(anyString());

        verify(rolePermissionRepository).findByRole(anyString());

        assertEquals(rolePermission.getRole(), userPermission.getRole());
    }

    @Test
    void getUserPermissionReturnPermission_ko() {
        Mockito.when(rolePermissionRepository.findByRole(anyString()))
                .thenReturn(Optional.empty());
        try {
            rolePermissionService.getUserPermission(anyString());
        } catch (PermissionNotFoundException e) {
          log.info("AuthorizationPermissionException: {}", e.getCode());
            assertEquals(RoleConstants.ExceptionCode.PERMISSIONS_NOT_FOUND, e.getCode());
        }
    }

}
