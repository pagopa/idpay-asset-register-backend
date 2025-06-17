package it.gov.pagopa.register.repository.role;

import it.gov.pagopa.register.model.role.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RolePermissionRepository extends MongoRepository<RolePermission, String> {

    Optional<RolePermission> findByRole (String role);
}
