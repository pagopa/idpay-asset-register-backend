package it.gov.pagopa.common.kafka.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CommonUtilitiesTest {

  @Data
  static class TestPayload {
    public String name;
    public int age;
  }


  @Test
  void testReadMessagePayload_withString() {
    Message<String> message = MessageBuilder.withPayload("{\"name\":\"Alice\",\"age\":30}").build();
    String payload = CommonUtilities.readMessagePayload(message);
    assertEquals("{\"name\":\"Alice\",\"age\":30}", payload);
  }

  @Test
  void testReadMessagePayload_withBytes() {
    byte[] bytes = "{\"name\":\"Bob\",\"age\":25}".getBytes();
    Message<byte[]> message = MessageBuilder.withPayload(bytes).build();
    String payload = CommonUtilities.readMessagePayload(message);
    assertEquals("{\"name\":\"Bob\",\"age\":25}", payload);
  }

  @Test
  void testDeserializeMessage_success() {
    String json = "{\"name\":\"Charlie\",\"age\":40}";
    Message<String> message = MessageBuilder.withPayload(json).build();
    ObjectReader reader = new ObjectMapper().readerFor(TestPayload.class);
    TestPayload expected = new TestPayload();
    expected.setAge(40);
    expected.setName("Charlie");
    AtomicReference<Throwable> errorRef = new AtomicReference<>();
    TestPayload result = CommonUtilities.deserializeMessage(message, reader, errorRef::set);

    assertNotNull(result);
    assertEquals(expected, result);
    assertNull(errorRef.get());
  }

  @Test
  void testDeserializeMessage_failure() {
    String invalidJson = "{\"name\":\"Dana\",\"age\":\"notANumber\"}";
    Message<String> message = MessageBuilder.withPayload(invalidJson).build();
    ObjectReader reader = new ObjectMapper().readerFor(TestPayload.class);

    AtomicReference<Throwable> errorRef = new AtomicReference<>();
    TestPayload result = CommonUtilities.deserializeMessage(message, reader, errorRef::set);

    assertNull(result);
    assertNotNull(errorRef.get());
    assertTrue(errorRef.get() instanceof JsonProcessingException);
  }
}
