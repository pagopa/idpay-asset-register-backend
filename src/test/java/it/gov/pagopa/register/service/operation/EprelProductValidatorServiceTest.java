package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.configuration.EprelValidationConfig;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.service.validator.EprelProductValidatorService;
import it.gov.pagopa.register.utils.EprelProduct;
import it.gov.pagopa.register.utils.EprelResult;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
  EprelValidationConfig.class,
  EprelConnector.class,
  EprelProductValidatorService.class
})
class EprelProductValidatorServiceTest {


  private EprelValidationConfig eprelValidationConfig;

  @MockitoBean
  private EprelConnector eprelConnector;

  @Autowired
  private EprelProductValidatorService validatorService;

  @Test
  void testValidateRecords_withValidAndInvalidRecords() {
    String category = "WASHERDRIERS";
    String orgId = "org123";
    String productFileId = "file123";

    CSVRecord validProductCsv = mock(CSVRecord.class);
    when(validProductCsv.get(CODE_EPREL)).thenReturn("valid-code");

    CSVRecord invalidProductCsv = mock(CSVRecord.class);
    when(invalidProductCsv.get(CODE_EPREL)).thenReturn("invalid-code");

    CSVRecord nullProductCsv = mock(CSVRecord.class);
    when(nullProductCsv.get(CODE_EPREL)).thenReturn("null-code");


    EprelProduct validProduct = new EprelProduct();
    validProduct.setEprelRegistrationNumber("valid-code");
    validProduct.setEnergyClass("B");
    validProduct.setOrgVerificationStatus("VERIFIED");
    validProduct.setTrademarkVerificationStatus("VERIFIED");
    validProduct.setBlocked(Boolean.FALSE);
    validProduct.setStatus("PUBLISHED");
    validProduct.setEnergyClassWash("A");
    validProduct.setProductGroup("WASHERDRIERS");

    EprelProduct invalidProduct = new EprelProduct();
    invalidProduct.setEprelRegistrationNumber("invalid-code");
    invalidProduct.setEprelRegistrationNumber("valid-code");
    invalidProduct.setEnergyClass("B");
    invalidProduct.setOrgVerificationStatus("VERIFIED");
    invalidProduct.setTrademarkVerificationStatus("VERIFIED");
    invalidProduct.setBlocked(Boolean.FALSE);
    invalidProduct.setStatus("PUBLISHED");
    invalidProduct.setEnergyClassWash("B");
    invalidProduct.setProductGroup("WASHERDRIERS");

    when(eprelConnector.callEprel("valid-code")).thenReturn(validProduct);
    when(eprelConnector.callEprel("invalid-code")).thenReturn(invalidProduct);
    when(eprelConnector.callEprel("null-code")).thenReturn(null);

    List<CSVRecord> records = List.of(validProductCsv, invalidProductCsv,nullProductCsv);

    EprelResult result = validatorService.validateRecords(records, EPREL_FIELDS, category, orgId, productFileId, null);

    assertEquals(1, result.getValidRecords().size());
    assertEquals(2, result.getInvalidRecords().size());
    assertEquals(2, result.getErrorMessages().size());
    assertEquals(ERROR_ENERGY_CLASS, result.getErrorMessages().get(invalidProductCsv));
    assertEquals("Product not found in EPREL", result.getErrorMessages().get(nullProductCsv));
  }
}
