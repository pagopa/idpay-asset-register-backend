package it.gov.pagopa.register.constants;

import it.gov.pagopa.register.model.operation.Product;

import java.util.Set;

public class AggregationConstants {

  private AggregationConstants(){}
  public static final String PRODUCT_COLLECTION_NAME = "product";
  public static final String FIELD_ENERGY_CLASS = "energyClass";
  public static final String FIELD_CATEGORY = "category";
  public static final String FIELD_PRODUCT_FILE_ID = "productFileId";
  public static final String FIELD_BATCH_NAME = "batchName";
  public static final String FIELD_ORGANIZATION_ID = "organizationId";
  public static final String FIELD_ID = "_id";
  public static final String FIELD_STATUS = "status";
  public static final String RUNTIME_FIELD_CATEGORY_IT = "categoryIt";
  public static final String FIELD_MOTIVTAION = "motivation";

  public static final String LOWER_SUFFIX = "_lower";

  public static final String FIELD_GTIN_DB = "_id";

  public static final Set<String> CASE_INSENSITIVE_FIELDS = Set.of(
    Product.Fields.organizationName,
    FIELD_GTIN_DB,
    Product.Fields.productName,
    Product.Fields.fullProductName,
    Product.Fields.brand,
    Product.Fields.model
  );
}
