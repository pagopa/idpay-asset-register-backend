package it.gov.pagopa.register.configuration.initiative.mapper;

import it.gov.pagopa.register.configuration.initiative.model.CategoryConfig;
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
import java.util.stream.Collectors;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.limitName;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.normalizeCsvCode;
import static it.gov.pagopa.register.utils.CsvUtils.DELIMITER;
import static it.gov.pagopa.register.utils.EprelUtils.mapEnergyClass;

@Component("EPREL")
public class EprelProductMapper implements ProductMapperStrategy {

  @Override
  public String extractBusinessKey(
    CSVRecord csvRecord,
    CategoryConfig categoryConfig
  ) {
    return normalizeCsvCode(
      csvRecord.get(categoryConfig.getInputIdentifierField())
    );
  }

  @Override
  public CSVRecord mapToCsvRow(Product product, List<String> headers) {
    try {
      StringWriter out = new StringWriter();
      CSVPrinter printer = new CSVPrinter(out, CSVFormat.Builder.create()
        .setHeader(headers.toArray(new String[0]))
        .setDelimiter(DELIMITER)
        .build());

      printer.printRecord(
        product.getEprelCode(),
        product.getGtinCode(),
        product.getProductCode(),
        product.getCategory(),
        product.getCountryOfProduction()
      );

      CSVFormat format = CSVFormat.Builder.create()
        .setHeader(headers.toArray(new String[0]))
        .setSkipHeaderRecord(true)
        .setDelimiter(DELIMITER)
        .setTrim(true)
        .build();
      List<CSVRecord> records = format.parse(new StringReader(out.toString())).getRecords();
      return records.isEmpty() ? null : records.getFirst();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Product mapToProduct(
    CSVRecord csvRecord,
    String category,
    String orgId,
    String initiativeId,
    String productFileId,
    String organizationName,
    MappingContext mappingContext
  ) {


    String normalizedCategory =
      category != null ? category.trim().replaceAll("\\s+", "") : null;

    String capacity = mapCapacity(normalizedCategory, mappingContext);

    String codeProduct = normalizeCsvCode(csvRecord.get(CODE_PRODUCT));
    String gtinCode = normalizeCsvCode(csvRecord.get(CODE_GTIN_EAN));

    String productName =
      limitName(mapName(null, mappingContext, normalizedCategory, capacity));

    String fullProductName =
      limitName(mapName(gtinCode, mappingContext, normalizedCategory, capacity));

    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .initiativeId(initiativeId)
      .registrationDate(LocalDateTime.now(ZoneOffset.UTC))
      .status(ProductStatus.UPLOADED.name())

      .productCode(codeProduct)
      .gtinCode(gtinCode)
      .eprelCode(csvRecord.get(CODE_EPREL))
      .category(normalizedCategory)

      .productGroup(mappingContext.getExternalData().get("productGroup").toString())
      .countryOfProduction(csvRecord.get(COUNTRY_OF_PRODUCTION))
      .brand(mappingContext.getExternalData().get("supplierOrTrademark").toString())
      .model(mappingContext.getExternalData().get("modelIdentifier").toString())
      .energyClass(mapEnergyClass(mappingContext.getExternalData().get("energyClass").toString()))
      .capacity(capacity)

      .productName(productName)
      .fullProductName(fullProductName)
      .organizationName(organizationName)

      .statusChangeChronology(new ArrayList<>())
      .formalMotivation("")
      .build();
  }

  private String mapCapacity(String category, MappingContext eprelData) {
    if (eprelData == null) return "N\\A";

    return switch (category) {
      case WASHINGMACHINES, TUMBLEDRYERS ->
        eprelData.getExternalData().get("ratedCapacity") != null
          ? eprelData.getExternalData().get("ratedCapacity").toString() + " kg"
          : "N\\A";

      case WASHERDRIERS ->
        eprelData.getExternalData().get("ratedCapacityWash") != null
          ? eprelData.getExternalData().get("ratedCapacityWash").toString() + " kg"
          : "N\\A";

      case OVENS -> {
        Object cavitiesObj = eprelData.getExternalData().get("cavities");

        if (cavitiesObj instanceof List<?> cavities && !cavities.isEmpty()) {
          yield cavities.stream()
            .map(c -> (EprelProduct.Cavity) c)
            .map(c -> c.getVolume() != null ? c.getVolume() + " l" : "N/A")
            .collect(Collectors.joining(" / "));
        }
        yield "N\\A";
      }

      case DISHWASHERS ->
        eprelData.getExternalData().get("ratedCapacity") != null
          ? eprelData.getExternalData().get("ratedCapacity") + " c"
          : "N\\A";

      case REFRIGERATINGAPPL ->
        eprelData.getExternalData().get("totalVolume") != null
          ? eprelData.getExternalData().get("totalVolume") + " l"
          : "N\\A";

      default -> "N\\A";
    };
  }

  private String mapName(
    String gtinOrNull,
    MappingContext eprel,
    String category,
    String capacity
  ) {
    String type = resolveProductType(eprel, category);

    StringBuilder sb = new StringBuilder();
    if (gtinOrNull != null && !gtinOrNull.isBlank()) {
      sb.append(gtinOrNull).append(" - ");
    }

    sb.append(type)
      .append(" ")
      .append(eprel.getExternalData().get("supplierOrTrademark").toString())
      .append(" ")
      .append(eprel.getExternalData().get("modelIdentifier").toString());

    if (!"N\\A".equals(capacity)) {
      sb.append(" ").append(capacity);
    }

    return sb.toString();
  }

  private String resolveProductType(MappingContext eprel, String category) {

    if (REFRIGERATINGAPPL.equals(category)) {

      Object compartmentsObj = eprel.getExternalData().get("compartments");

      if (compartmentsObj instanceof List<?> compartments && !compartments.isEmpty()) {

        boolean isRefrigerator = compartments.stream()
          .map(c -> (EprelProduct.RefrigeratorCompartment) c)
          .anyMatch(c -> {
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
          });

        return isRefrigerator ? REFRIGERATOR_IT : FREEZER_IT;
      }
      return FREEZER_IT;
    }

    return CATEGORIES_TO_IT_S.get(category);
  }


}
