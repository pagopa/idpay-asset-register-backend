package it.gov.pagopa.register.utils;

import it.gov.pagopa.register.model.operation.Product;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.ERROR_MAP;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.ErrorKey.DIFFERENT_ORGANIZATIONID;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.ErrorKey.STATUS_NOT_VALID;


@Slf4j
public class ValidationUtils {

  private ValidationUtils(){}
  public static boolean dbCheck(String orgId, CSVRecord csvRecord, Optional<Product> optProduct, List<CSVRecord> invalidRecords, Map<CSVRecord, String> errorMessages, List<String> allowedReloadStatuses,String dmDate) {
    boolean isProductPresent = optProduct.isPresent();
    boolean dbCheck = true;
    if (isProductPresent) {
      if (!orgId.equals(optProduct.get().getOrganizationId())) {
        addError(csvRecord, ERROR_MAP.get(DIFFERENT_ORGANIZATIONID), invalidRecords, errorMessages);
        dbCheck = false;
      }
      else if (!allowedReloadStatuses.contains(optProduct.get().getStatus())) {
        addError(csvRecord, ERROR_MAP.get(STATUS_NOT_VALID).replace("{}",dmDate), invalidRecords, errorMessages);
        dbCheck = false;
      }
    }
    return dbCheck;
  }

  public static void addError(CSVRecord csvRecord, String message, List<CSVRecord> invalidRecords, Map<CSVRecord, String> errorMessages) {
    invalidRecords.add(csvRecord);
    errorMessages.put(csvRecord, message);
  }


}
