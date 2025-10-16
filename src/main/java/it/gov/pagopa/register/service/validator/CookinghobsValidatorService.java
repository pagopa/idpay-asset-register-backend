package it.gov.pagopa.register.service.validator;


import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.mapCookingHobToProduct;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.mapProductToCsvRow;
import static it.gov.pagopa.register.utils.ValidationUtils.dbCheck;

@Component
@RequiredArgsConstructor
@Slf4j
public class CookinghobsValidatorService {

  private final ProductRepository productRepository;


  public ProductValidationResult validateRecords(List<CSVRecord> records, String orgId, String productFileId, List<String> headers, String organizationName) {
    Map<String, Product> validProduct = new LinkedHashMap<>();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();
    for (CSVRecord csvRecord : records) {
      Optional<Product> optProduct = productRepository.findById(csvRecord.get(CODE_GTIN_EAN));
      boolean dbCheck = dbCheck(orgId, csvRecord, optProduct, invalidRecords, errorMessages);
      if (dbCheck) {
        if (validProduct.containsKey(csvRecord.get(CODE_GTIN_EAN))) {
          Product duplicateGtin = validProduct.remove(csvRecord.get(CODE_GTIN_EAN));
          CSVRecord duplicateGtinRow = mapProductToCsvRow(duplicateGtin, COOKINGHOBS, headers);
          invalidRecords.add(duplicateGtinRow);
          errorMessages.put(duplicateGtinRow, DUPLICATE_GTIN_EAN);
          log.info("[PRODUCT_UPLOAD] - Duplicate error for record with GTIN code: {}", csvRecord.get(CODE_GTIN_EAN));
        }
        log.info("[PRODUCT_UPLOAD] - Mapping product with GTIN code: {}", csvRecord.get(CODE_GTIN_EAN));
        Product product = mapCookingHobToProduct(csvRecord, orgId, productFileId, organizationName);
        optProduct.ifPresent(dbProduct -> {
          product.setFormalMotivation(dbProduct.getFormalMotivation());
          product.setStatusChangeChronology(dbProduct.getStatusChangeChronology());
        });
        log.info("[PRODUCT_UPLOAD] - Mapped product: {}", product.toString());
        validProduct.put(csvRecord.get(CODE_GTIN_EAN), product);
        log.info("[PRODUCT_UPLOAD] - Added cooking hob product: {}", csvRecord.get(CODE_GTIN_EAN));
      }
    }
    return new ProductValidationResult(validProduct,invalidRecords,errorMessages);
  }



}
