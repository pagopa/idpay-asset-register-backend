package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.configuration.EprelValidationConfig;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.dto.utils.EprelResult;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

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



  @MockitoBean
  private EprelConnector eprelConnector;

  @Autowired
  private EprelProductValidatorService validatorService;

  @MockitoBean
  private ProductRepository productRepository;

  @Test
  void testValidateRecords_withValidAndInvalidRecords() {
    String category = "WASHERDRIERS";
    String orgId = "org123";
    String productFileId = "file123";

    CSVRecord wrongOrgIdCsv = mock(CSVRecord.class);
    when(wrongOrgIdCsv.get(CODE_EPREL)).thenReturn("valid-code");
    when(wrongOrgIdCsv.get(CODE_GTIN_EAN)).thenReturn("wrong-org-id-csv");

    CSVRecord wrongStatusCsv = mock(CSVRecord.class);
    when(wrongStatusCsv.get(CODE_EPREL)).thenReturn("valid-code");
    when(wrongStatusCsv.get(CODE_GTIN_EAN)).thenReturn("wrong-status-csv");

    CSVRecord validProductCsv = mock(CSVRecord.class);
    when(validProductCsv.get(CODE_EPREL)).thenReturn("valid-code");
    when(validProductCsv.get(CODE_GTIN_EAN)).thenReturn("valid-gtin");

    CSVRecord duplicatedProductCsv = mock(CSVRecord.class);
    when(duplicatedProductCsv.get(CODE_EPREL)).thenReturn("valid-code-2");
    when(duplicatedProductCsv.get(CODE_GTIN_EAN)).thenReturn("valid-gtin");

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

    Product productWrongId = Product.builder()
      .organizationId("test")
      .status("APPROVED")
      .build();

    Product productWrontStatus = Product.builder()
      .organizationId(orgId)
      .status("REJECTED")
      .motivation("Motivation")
      .build();

    when(eprelConnector.callEprel("valid-code")).thenReturn(validProduct);
    when(eprelConnector.callEprel("valid-code-2")).thenReturn(validProduct);
    when(eprelConnector.callEprel("invalid-code")).thenReturn(invalidProduct);
    when(eprelConnector.callEprel("null-code")).thenReturn(null);

    List<CSVRecord> records = List.of(
      validProductCsv,
      invalidProductCsv,
      nullProductCsv,
      duplicatedProductCsv,
      wrongOrgIdCsv,
      wrongStatusCsv
    );

    when(productRepository.findById("wrong-org-id-csv")).thenReturn(Optional.of(productWrongId));
    when(productRepository.findById("wrong-status-csv")).thenReturn(Optional.of(productWrontStatus));

    EprelResult result = validatorService.validateRecords(records, EPREL_FIELDS, category, orgId, productFileId, null,"orgName");

    assertEquals(1, result.getValidRecords().size());
    assertEquals(5, result.getInvalidRecords().size());
    assertEquals(5, result.getErrorMessages().size());

  }
}
