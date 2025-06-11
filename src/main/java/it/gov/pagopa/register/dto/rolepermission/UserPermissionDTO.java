package it.gov.pagopa.register.dto.rolepermission;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

@JsonPropertyOrder({
  UserPermissionDTO.JSON_PROPERTY_INSTITUTION,
  UserPermissionDTO.JSON_PROPERTY_PERMISSIONS
})
@Data
public class UserPermissionDTO {
  public static final String JSON_PROPERTY_INSTITUTION = "institution";
  private String institution;

  public static final String JSON_PROPERTY_PERMISSIONS = "permissions";
  private List<PermissionDTO> permissions = null;
}

