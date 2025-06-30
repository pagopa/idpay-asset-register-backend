package it.gov.pagopa.register.repository.role;

import it.gov.pagopa.register.model.role.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

public interface ProductSpecificRepository {

  List<Product> findByFilter(Criteria criteria, Pageable pageable);
}
