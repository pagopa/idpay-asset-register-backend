package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.dto.operation.EmailProductDTO;
import it.gov.pagopa.register.model.operation.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

public interface ProductSpecificRepository {

  List<Product> findByFilter(Criteria criteria, Pageable pageable);

  Criteria getCriteria(String organizationId,
                       String category,
                       String productFileId,
                       String eprelCode,
                       String gtinCode,
                       String productName,
                       String status);

  Long getCount(Criteria criteria);

  List<Product> retrieveDistinctProductFileIdsBasedOnRole(String organizationId, String organizationSelected, String role);

  List<Product> findByIdsAndValidStatusByRole(List<String> productIds, String targetStatus, String role);

  List<EmailProductDTO> getProductNamesGroupedByEmail(List<String> gtinCodes);
}
