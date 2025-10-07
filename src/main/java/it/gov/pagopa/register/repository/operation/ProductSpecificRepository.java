package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.dto.operation.EmailProductDTO;
import it.gov.pagopa.register.dto.operation.ProductCriteriaDTO;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.model.operation.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

public interface ProductSpecificRepository {

  List<Product> findByFilter(Criteria criteria, Pageable pageable);

  Criteria getCriteria(ProductCriteriaDTO criteria);

  Long getCount(Criteria criteria);

  List<Product> retrieveDistinctProductFileIdsBasedOnRole(String organizationId, String organizationSelected, String role);

  List<Product> findUpdatableProducts(List<String> productIds, ProductStatus currentStatus, ProductStatus targetStatus, String role);

  List<EmailProductDTO> getProductNamesGroupedByEmail(List<String> gtinCodes);

  List<Product> findByIds(List<String> productIds);

  List<String> getAllowedInitialStates(ProductStatus targetStatus, String role);

}
