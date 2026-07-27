package it.gov.pagopa.register.mapper.product;

import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
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

import static it.gov.pagopa.register.constants.AssetRegisterConstants.Category.Labels.CATEGORIES_FOR_PRODUCT_NAME;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.CsvHeader.*;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.*;
import static it.gov.pagopa.register.utils.CsvUtils.DELIMITER;


@SuppressWarnings({"java:S6830", "java:S117"})
@Component("DECODER")
public class DecoderProductMapper implements ProductMapperStrategy {

  @Override
  public String extractBusinessKey(CSVRecord csvRecord, CategoryConfig config) {
    return normalizeCsvCode(csvRecord.get(config.getInputIdentifierField()));
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

    NormalizedInput input = normalize(csvRecord);

    String productName = buildProductName(category, input.brand, input.model);
    String fullProductName = buildFullProductName(input.gtinCode, productName);

    return Product.builder()
      .id(buildId(input.gtinCode, initiativeId))
      .gtinCode(input.gtinCode)
      .productCode(input.productCode)
      .brand(input.brand)
      .model(input.model)
      .category(category)
      .productName(limitName(productName))
      .fullProductName(limitName(fullProductName))
      .productFileId(productFileId)
      .organizationId(orgId)
      .organizationName(organizationName)
      .initiativeId(initiativeId)
      .capacity("N\\A")
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
          product.getGtinCode(),
          product.getProductCode(),
          product.getCategory(),
          product.getModel(),
          product.getBrand()
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
      throw new IllegalStateException("Errore nella generazione CSV per DECODER", e);
    }
  }


  private NormalizedInput normalize(CSVRecord csv) {
    return new NormalizedInput(
      sanitizeGtinForDto(csv.get(CODE_GTIN_EAN)),
      sanitizeProductCodeForDto(csv.get(CODE_PRODUCT)),
      sanitizeBrandOrModelForDto(csv.get(BRAND)),
      sanitizeBrandOrModelForDto(csv.get(MODEL))
    );
  }


  private String buildProductName(String category, String brand, String model) {
    return CATEGORIES_FOR_PRODUCT_NAME.get(category) + " " + brand + " " + model;
  }

  private String buildFullProductName(String gtin, String productName) {
    return gtin + " - " + productName;
  }

  private String buildId(String gtin, String initiativeId) {
    return gtin + "_" + initiativeId;
  }


  private record NormalizedInput(
    String gtinCode,
    String productCode,
    String brand,
    String model
  ) {}
}
