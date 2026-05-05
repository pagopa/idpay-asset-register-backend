package it.gov.pagopa.register.configuration.initiative.mapper;

import it.gov.pagopa.register.configuration.initiative.model.CategoryConfig;
import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVRecord;

import java.util.List;

public interface ProductMapperStrategy {

  /**
   * Chiave di business del prodotto (GTIN, codice, ecc.)
   */

  String extractBusinessKey(
    CSVRecord record,
    CategoryConfig categoryConfig
  );


  /**
   * Mapping CSV → Product
   */

  Product mapToProduct(
    CSVRecord csvRecord,
    String category,
    String orgId,
    String initiativeId,
    String productFileId,
    String organizationName,
    MappingContext context
  );


  /**
   * Mapping Product → CSVRecord (per error report)
   */
  CSVRecord mapToCsvRow(
      Product product,
      List<String> headers
  );
}
