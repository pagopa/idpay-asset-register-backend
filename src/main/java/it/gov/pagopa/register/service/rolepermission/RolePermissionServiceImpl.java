package it.gov.pagopa.register.service.rolepermission;

import it.gov.pagopa.register.constants.RolePermissionConstants;
import it.gov.pagopa.register.dto.rolepermission.PermissionDTO;
import it.gov.pagopa.register.dto.rolepermission.UserPermissionDTO;
import it.gov.pagopa.register.exception.rolepermission.PermissionNotFoundException;
import it.gov.pagopa.register.model.rolepermission.Permission;
import it.gov.pagopa.register.model.rolepermission.RolePermission;
import it.gov.pagopa.register.repository.rolepermission.RolePermissionRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    public RolePermissionServiceImpl(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public UserPermissionDTO getUserPermission(String institutionType) {
        RolePermission roleOptional = rolePermissionRepository.findByInstitution(institutionType).orElseThrow(() ->
                new PermissionNotFoundException(String.format(RolePermissionConstants.PERMISSIONS_NOT_FOUND_MSG, institutionType))
        );
        return rolePermissionToDTO(roleOptional);
    }

    private UserPermissionDTO rolePermissionToDTO(RolePermission rolePermission) {
        UserPermissionDTO userPermissionDTO = new UserPermissionDTO();
        userPermissionDTO.setInstitution(rolePermission.getInstitution());
        List<PermissionDTO> permissionDTOList = new ArrayList<>();
        if(rolePermission.getPermissions() != null) {
            for (Permission source : rolePermission.getPermissions()) {
                PermissionDTO permissionDTO = new PermissionDTO();
                BeanUtils.copyProperties(source, permissionDTO);
                permissionDTOList.add(permissionDTO);
            }
        }
        userPermissionDTO.setPermissions(permissionDTOList);
        return userPermissionDTO;
    }

}
