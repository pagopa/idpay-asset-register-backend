package it.gov.pagopa.register.model.rolepermission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    private String name;

    private String description;

    private String mode;
}
