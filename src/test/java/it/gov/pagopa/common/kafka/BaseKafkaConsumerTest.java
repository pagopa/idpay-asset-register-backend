package it.gov.pagopa.common.kafka;

import it.gov.pagopa.common.kafka.utils.BaseConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseKafkaConsumerTest {

  private static final String APPLICATION_NAME = "myApp";

  private BaseConsumer spyConsumer;
  @BeforeEach
  void setUp() {
    BaseConsumer consumer = new BaseConsumer(APPLICATION_NAME);
    spyConsumer = Mockito.spy(consumer);
  }


  @Test
  void testExecute_isRetryFromOther2AppsTrue() {

    String payload = "test-message";
    Acknowledgment acknowledgment = mock(Acknowledgment.class);
    Message<String> message = MessageBuilder.withPayload(payload)
      .setHeader("applicationName", "notMyApp".getBytes(StandardCharsets.UTF_8))
      .setHeader("kafka_acknowledgment", acknowledgment)
      .build();

    spyConsumer.execute(message);
    verify(spyConsumer,never()).execute(any(), eq(message));
    verify(acknowledgment).acknowledge();
  }

  @Test
  void testExecute_ThrowsRuntimeException() {
    String payload = "\"exception\"";
    Acknowledgment acknowledgment = mock(Acknowledgment.class);
    Message<String> message = MessageBuilder.withPayload(payload)
      .setHeader("applicationName", APPLICATION_NAME.getBytes(StandardCharsets.UTF_8))
      .setHeader("kafka_acknowledgment", acknowledgment)
      .build();

    spyConsumer.execute(message);
    verify(spyConsumer).execute(any(), eq(message));
    verify(acknowledgment).acknowledge();
  }






}
