package it.gov.pagopa.register.mapper.product;

import it.gov.pagopa.register.model.initiative.CategoryConfig;
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

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.limitName;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.normalizeCsvCode;
import static it.gov.pagopa.register.utils.CsvUtils.DELIMITER;


@SuppressWarnings({"java:S6830", "java:S117"})
@Component("DECODER")
public class DecoderProductMapper implements ProductMapperStrategy {

  @Override
  public String extractBusinessKey(CSVRecord csvRecord, CategoryConfig config) {
    return csvRecord.get(config.getInputIdentifierField());
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

    String codeProduct = normalizeCsvCode(csvRecord.get(CODE_PRODUCT));
    String gtinCode = normalizeCsvCode(csvRecord.get(CODE_GTIN_EAN));

    String productName = CATEGORIES_TO_IT_S.get(category) + " " + csvRecord.get(BRAND) + " " + csvRecord.get(MODEL);
    String fullProductName = gtinCode + " - " + productName;

    return Product.builder()
      .id(gtinCode+"_"+initiativeId)
      .gtinCode(gtinCode)
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now(ZoneOffset.UTC))
      .status(ProductStatus.UPLOADED.name())
      .productCode(codeProduct)
      .initiativeId(initiativeId)
      .gtinCode(gtinCode)
      .category(category)
      .brand(csvRecord.get(BRAND))
      .model(csvRecord.get(MODEL))
      .capacity("N\\A")
      .productName(limitName(productName))
      .fullProductName(limitName(fullProductName))
      .organizationName(organizationName)
      .statusChangeChronology(new ArrayList<>())
      .formalMotivation("")

      .build();
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
        product.getGtinCode(),
        product.getProductCode(),
        product.getCategory(),
        product.getModel(),
        product.getBrand()
      );

      CSVFormat format = CSVFormat.Builder.create()
        .setHeader(headers.toArray(new String[0]))
        .setSkipHeaderRecord(true)
        .setDelimiter(DELIMITER)
        .setTrim(true)
        .build();
      List<CSVRecord> records = format.parse(new StringReader(out.toString())).getRecords();
      return records.getFirst();
    } catch (Exception _) {
      return null;
    }
  }
}
