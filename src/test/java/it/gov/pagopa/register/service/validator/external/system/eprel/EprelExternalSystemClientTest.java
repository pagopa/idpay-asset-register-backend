package it.gov.pagopa.register.service.validator.external.system.eprel;

import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.model.initiative.ExternalCheckTemplate;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
  classes = {
    EprelExternalSystemClient.class,
  }
)
class EprelExternalSystemClientTest {

  @MockitoBean
  private EprelConnector eprelConnector;
  @Autowired
  private EprelExternalSystemClient client;

  @Test
  void shouldReturnSupportedType() {
    assertEquals("EPREL", client.supports());
  }


  @Test
  void shouldThrowExceptionWhenEprelCodeMissing() {

    CSVRecord csvRecord = mock(CSVRecord.class);
    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);

    when(template.getInputField()).thenReturn("eprelCode");
    when(csvRecord.get("eprelCode")).thenReturn(""); // vuoto

    assertThrows(IllegalArgumentException.class,
      () -> client.fetch(csvRecord, template)
    );
  }


  @Test
  void shouldThrowExceptionWhenConnectorReturnsNull() {

    CSVRecord csvRecord = mock(CSVRecord.class);
    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);

    when(template.getInputField()).thenReturn("eprelCode");
    when(csvRecord.get("eprelCode")).thenReturn("123");

    when(eprelConnector.callEprel("123")).thenReturn(null);

    assertThrows(IllegalStateException.class,
      () -> client.fetch(csvRecord, template)
    );
  }


  @Test
  void shouldReturnMappedExternalData() {

    CSVRecord csvRecord = mock(CSVRecord.class);
    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);

    when(template.getInputField()).thenReturn("eprelCode");
    when(csvRecord.get("eprelCode")).thenReturn("123");

    EprelProduct product = mock(EprelProduct.class);

    when(product.getStatus()).thenReturn("ACTIVE");
    when(product.getEnergyClass()).thenReturn("A");
    when(product.getEnergyClassWash()).thenReturn("B");
    when(product.getBlocked()).thenReturn(false);
    when(product.getProductGroup()).thenReturn("GROUP");
    when(product.getOrgVerificationStatus()).thenReturn("OK");
    when(product.getTrademarkVerificationStatus()).thenReturn("OK");
    when(product.getSupplierOrTrademark()).thenReturn("SUPPLIER");
    when(product.getModelIdentifier()).thenReturn("MODEL");
    when(product.getRatedCapacity()).thenReturn(String.valueOf(10));
    when(product.getRatedCapacityWash()).thenReturn(String.valueOf(8));
    when(product.getCavities()).thenReturn(List.of(new EprelProduct.Cavity(2)));
    when(product.getTotalVolume()).thenReturn(String.valueOf(100));

    when(eprelConnector.callEprel("123")).thenReturn(product);

    Map<String, Object> result = client.fetch(csvRecord, template);

    assertEquals("ACTIVE", result.get("status"));
    assertEquals("A", result.get("energyClass"));
    assertEquals("B", result.get("energyClassWash"));
    assertEquals(false, result.get("blocked"));
    assertEquals("GROUP", result.get("productGroup"));
    assertEquals("OK", result.get("orgVerificationStatus"));
    assertEquals("OK", result.get("trademarkVerificationStatus"));
    assertEquals("SUPPLIER", result.get("supplierOrTrademark"));
    assertEquals("MODEL", result.get("modelIdentifier"));
    assertEquals("10", result.get("getRatedCapacity"));
    assertEquals("8", result.get("getRatedCapacityWas"));
    assertEquals("100", result.get("totalVolume"));

    List<?> cavities = (List<?>) result.get("cavities");

    assertEquals(1, cavities.size());
    assertEquals(2, ((EprelProduct.Cavity) cavities.getFirst()).getVolume());

  }



  @Test
  void shouldThrowExceptionWhenCsvFieldMissing() {

    CSVRecord csvRecord = mock(CSVRecord.class);
    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);

    when(template.getInputField()).thenReturn("missingField");

    when(csvRecord.get("missingField"))
      .thenThrow(new IllegalArgumentException("Column not found"));

    assertThrows(IllegalArgumentException.class,
      () -> client.fetch(csvRecord, template)
    );
  }
}
