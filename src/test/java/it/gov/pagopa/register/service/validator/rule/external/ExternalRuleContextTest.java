package it.gov.pagopa.register.service.validator.rule.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExternalRuleContextTest {

  @Test
  void testExternalRuleContextCreationAndGetters() {
    Map<String, Object> externalData = new HashMap<>();
    Map<String, Object> categoryParameters = new HashMap<>();
    String category = "Welfare";

    ExternalRuleContext context = new ExternalRuleContext(externalData, categoryParameters, category);

    assertNotNull(context);
    assertEquals(externalData, context.getExternalData());
    assertEquals(categoryParameters, context.getCategoryParameters());
    assertEquals(category, context.getCategory());
  }

  @Test
  void testGetExternalValue() {
    Map<String, Object> externalData = new HashMap<>();
    externalData.put("fieldKey", "fieldValue");

    ExternalRuleContext context = new ExternalRuleContext(externalData, new HashMap<>(), "Welfare");

    assertEquals("fieldValue", context.getExternalValue("fieldKey"));
    assertNull(context.getExternalValue("unknownKey"));
  }

  @Test
  void testGetCategoryParameter_NotNull() {
    Map<String, Object> categoryParameters = new HashMap<>();
    categoryParameters.put("paramKey", "paramValue");

    ExternalRuleContext context = new ExternalRuleContext(new HashMap<>(), categoryParameters, "Welfare");

    assertEquals("paramValue", context.getCategoryParameter("paramKey"));
  }

  @Test
  void testGetCategoryParameter_Null() {
    ExternalRuleContext context = new ExternalRuleContext(new HashMap<>(), new HashMap<>(), "Welfare");

    assertNull(context.getCategoryParameter("unknownKey"));
  }

  @Test
  void testLombokMethods() {
    Map<String, Object> data = new HashMap<>();
    Map<String, Object> params = new HashMap<>();

    ExternalRuleContext context1 = new ExternalRuleContext(data, params, "Welfare");
    ExternalRuleContext context2 = new ExternalRuleContext(data, params, "Welfare");
    ExternalRuleContext context3 = new ExternalRuleContext(null, null, "Other");

    assertEquals(context1, context2);
    org.junit.jupiter.api.Assertions.assertNotEquals(context1, context3);
    assertEquals(context1.hashCode(), context2.hashCode());
    assertNotNull(context1.toString());
  }
}
