package it.gov.pagopa.register.repository.rolepermission;

import it.gov.pagopa.register.model.rolepermission.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RolePermissionRepository extends MongoRepository<RolePermission, String> {

    Optional<RolePermission> findByInstitution(String institutionType);
}
