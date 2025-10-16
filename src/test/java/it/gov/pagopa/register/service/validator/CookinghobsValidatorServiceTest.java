package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.enums.ProductStatus;
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

import static it.gov.pagopa.register.constants.AssetRegisterConstants.CODE_GTIN_EAN;
import static it.gov.pagopa.register.utils.ObjectMaker.buildStatusChangeEventsList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
  CookinghobsValidatorService.class
})
class CookinghobsValidatorServiceTest {

  @Autowired
  private CookinghobsValidatorService validatorService;

  @MockitoBean
  private ProductRepository productRepository;

  @Test
  void testValidateRecords_withValidAndInvalidCookingHobs() {
    String orgId = "org123";
    String productFileId = "file123";
    String organizationName = "orgName";
    List<String> headers = List.of("GTIN", "Name", "Brand");

    CSVRecord validCsv = mock(CSVRecord.class);
    when(validCsv.get(CODE_GTIN_EAN)).thenReturn("valid-gtin");

    CSVRecord duplicateCsv = mock(CSVRecord.class);
    when(duplicateCsv.get(CODE_GTIN_EAN)).thenReturn("valid-gtin");

    CSVRecord wrongOrgCsv = mock(CSVRecord.class);
    when(wrongOrgCsv.get(CODE_GTIN_EAN)).thenReturn("wrong-org");

    CSVRecord wrongStatusCsv = mock(CSVRecord.class);
    when(wrongStatusCsv.get(CODE_GTIN_EAN)).thenReturn("wrong-status");

    Product validProduct = Product.builder()
      .organizationId(orgId)
      .status(ProductStatus.UPLOADED.name())
      .formalMotivation("formal motivation")
      .statusChangeChronology(null)
      .build();

    Product productWrongOrg = Product.builder()
      .organizationId("otherOrg")
      .status(ProductStatus.UPLOADED.name())
      .build();

    Product productWrongStatus = Product.builder()
      .organizationId(orgId)
      .status(ProductStatus.APPROVED.name())
      .statusChangeChronology(buildStatusChangeEventsList())
      .build();

    when(productRepository.findById("valid-gtin")).thenReturn(Optional.of(validProduct));
    when(productRepository.findById("wrong-org")).thenReturn(Optional.of(productWrongOrg));
    when(productRepository.findById("wrong-status")).thenReturn(Optional.of(productWrongStatus));

    List<CSVRecord> records = List.of(validCsv, duplicateCsv, wrongOrgCsv, wrongStatusCsv);

    ProductValidationResult result = validatorService.validateRecords(records, orgId, productFileId, headers, organizationName);

    assertEquals(1, result.getValidRecords().size());
    assertEquals(3, result.getInvalidRecords().size());
    assertEquals(3, result.getErrorMessages().size());
  }


}
