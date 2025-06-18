package it.gov.pagopa.register.dto.role;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

@JsonPropertyOrder({
  UserPermissionDTO.JSON_PROPERTY_ROLE,
  UserPermissionDTO.JSON_PROPERTY_PERMISSIONS
})
@Data
public class UserPermissionDTO {
  public static final String JSON_PROPERTY_ROLE = "role";
  private String role;

  public static final String JSON_PROPERTY_PERMISSIONS = "permissions";
  private List<PermissionDTO> permissions = null;
}

