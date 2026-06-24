package it.gov.pagopa.register.mapper.product;

import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.Category.*;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.Category.Labels.CATEGORIES_FOR_PRODUCT_NAME;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.CsvHeader.*;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.EprelField.*;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.Refrigerator.*;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.*;
import static it.gov.pagopa.register.utils.CsvUtils.DELIMITER;
import static it.gov.pagopa.register.utils.EprelUtils.mapEnergyClass;


@SuppressWarnings({"java:S6830", "java:S117"})
@Component("EPREL")
public class EprelProductMapper implements ProductMapperStrategy {

  @Override
  public String extractBusinessKey(CSVRecord csvRecord, CategoryConfig categoryConfig) {
    return normalizeCsvCode(csvRecord.get(categoryConfig.getInputIdentifierField()));
  }

  @Override
  public Product mapToProduct(
    CSVRecord csvRecord,
    String category,
    String orgId,
    String initiativeId,
    String productFileId,
    String organizationName,
    MappingContext context
  ) {

    NormalizedInput input = normalize(csvRecord, category, context);

    String capacity = mapCapacity(input.category, input.externalData);
    String productName = limitName(buildName(null, input, capacity));
    String fullProductName = limitName(buildName(input.gtinCode, input, capacity));

    return Product.builder()
      .id(buildId(input.gtinCode, initiativeId))
      .gtinCode(input.gtinCode)
      .productCode(input.productCode)
      .eprelCode(input.eprelCode)
      .category(input.category)

      .brand(input.brand)
      .model(input.model)
      .energyClass(input.energyClass)
      .productGroup(input.productGroup)
      .countryOfProduction(input.country)

      .capacity(capacity)
      .productName(productName)
      .fullProductName(fullProductName)

      .productFileId(productFileId)
      .organizationId(orgId)
      .organizationName(organizationName)
      .initiativeId(initiativeId)

      .registrationDate(LocalDateTime.now(ZoneOffset.UTC))
      .status(ProductStatus.UPLOADED.name())

      .statusChangeChronology(new ArrayList<>())
      .formalMotivation("")
      .build();
  }

  @Override
  public CSVRecord mapToCsvRow(Product product, List<String> headers) {
    try {
      StringWriter out = new StringWriter();

      try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.Builder.create()
        .setHeader(headers.toArray(new String[0]))
        .setDelimiter(DELIMITER)
        .build())) {

        printer.printRecord(
          product.getEprelCode(),
          product.getGtinCode(),
          product.getProductCode(),
          product.getCategory(),
          product.getCountryOfProduction()
        );
      }

      CSVFormat format = CSVFormat.Builder.create()
        .setHeader(headers.toArray(new String[0]))
        .setSkipHeaderRecord(true)
        .setDelimiter(DELIMITER)
        .setTrim(true)
        .build();

      return format.parse(new StringReader(out.toString()))
        .getRecords()
        .getFirst();

    } catch (Exception e) {
      throw new IllegalStateException("Errore CSV EPREL", e);
    }
  }

  private NormalizedInput normalize(CSVRecord csv, String category, MappingContext ctx) {

    var ext = ctx.getExternalData();

    return new NormalizedInput(
      sanitizeGtinForDto(csv.get(CODE_GTIN_EAN)),
      sanitizeProductCodeForDto(csv.get(CODE_PRODUCT)),
      normalizeCsvCode(csv.get(CODE_EPREL)),
      normalizeCategory(category),
      normalizeCountry(csv.get(COUNTRY_OF_PRODUCTION)),
      safe(ext.get("supplierOrTrademark")),
      safe(ext.get("modelIdentifier")),
      mapEnergyClass(safe(ext.get("energyClass"))),
      safe(ext.get("productGroup")),
      ext
    );
  }

  private String normalizeCategory(String c) {
    return c == null ? null : c.trim().replaceAll("\\s+", "");
  }

  private String normalizeCountry(String c) {
    return c == null ? null : c.trim().toUpperCase();
  }

  private String safe(Object o) {
    return o == null ? null : o.toString().trim();
  }

  private String buildName(String gtin, NormalizedInput input, String capacity) {

    String type = resolveProductType(input);

    StringBuilder sb = new StringBuilder();

    if (gtin != null && !gtin.isBlank()) {
      sb.append(gtin).append(" - ");
    }

    sb.append(type)
      .append(" ")
      .append(input.brand)
      .append(" ")
      .append(input.model);

    if (!"N\\A".equals(capacity)) {
      sb.append(" ").append(capacity);
    }

    return sb.toString();
  }

  private String buildId(String gtin, String initiativeId) {
    return gtin + "_" + initiativeId;
  }

  private String mapCapacity(String category, Map<String, Object> data) {
    if (data == null) return "N\\A";

    return switch (category) {
      case WASHINGMACHINES, TUMBLEDRYERS ->
        data.get(RATED_CAPACITY) != null ? data.get(RATED_CAPACITY) + " kg" : "N\\A";

      case WASHERDRIERS ->
        data.get(RATED_CAPACITY_WASH) != null ? data.get(RATED_CAPACITY_WASH) + " kg" : "N\\A";

      case OVENS -> extractOvenCapacity(data);

      case DISHWASHERS ->
        data.get(RATED_CAPACITY) != null ? data.get(RATED_CAPACITY) + " c" : "N\\A";

      case REFRIGERATINGAPPL ->
        data.get(TOTAL_VOLUME) != null ? data.get(TOTAL_VOLUME) + " l" : "N\\A";

      default -> "N\\A";
    };
  }

  private String extractOvenCapacity(Map<String, Object> data) {
    Object cavitiesObj = data.get(CAVITIES);

    if (cavitiesObj instanceof List<?> cavities && !cavities.isEmpty()) {
      return cavities.stream()
        .map(c -> (EprelProduct.Cavity) c)
        .map(c -> c.getVolume() != null ? c.getVolume() + " l" : "N\\A")
        .collect(Collectors.joining(" / "));
    }

    return "N\\A";
  }
  private String resolveProductType(NormalizedInput input) {

    if (REFRIGERATINGAPPL.equals(input.category)) {
      return resolveRefrigeratingType(input.externalData);
    }

    return CATEGORIES_FOR_PRODUCT_NAME.get(input.category);
  }

  private String resolveRefrigeratingType(Map<String, Object> data) {
    Object obj = data.get("compartments");

    if (obj instanceof List<?> compartments && !compartments.isEmpty()) {
      boolean isFridge = compartments.stream()
        .map(c -> (EprelProduct.RefrigeratorCompartment) c)
        .anyMatch(this::isRefrigerator);

      return isFridge ? REFRIGERATOR_IT : FREEZER_IT;
    }

    return FREEZER_IT;
  }

  private boolean isRefrigerator(EprelProduct.RefrigeratorCompartment c) {

    if (REFRIGERATORS_CATEGORY.contains(c.getCompartmentType())) {
      return true;
    }

    if (VARIABLE_TEMP.equals(c.getCompartmentType())) {
      return c.getSubCompartments() != null &&
        c.getSubCompartments().stream()
          .map(EprelProduct.SubCompartment::getCompartmentType)
          .anyMatch(REFRIGERATORS_CATEGORY::contains);
    }

    return false;
  }

  private record NormalizedInput(
    String gtinCode,
    String productCode,
    String eprelCode,
    String category,
    String country,
    String brand,
    String model,
    String energyClass,
    String productGroup,
    Map<String, Object> externalData
  ) {}
}
