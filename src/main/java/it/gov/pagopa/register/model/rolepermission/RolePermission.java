package it.gov.pagopa.register.model.rolepermission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document("role_permission")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    @Id
    private String id;

    private String institution;

    private String description;

    private List<Permission> permissions;

}
