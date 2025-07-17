package it.gov.pagopa.common.kafka;

import it.gov.pagopa.common.kafka.utils.BaseConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;


import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseKafkaConsumerTest {

  private static final String APPLICATION_NAME = "myApp";
  private BaseConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new BaseConsumer(APPLICATION_NAME) {};
  }

  @Test
  void testExecute_processMessage() {

    String payload = "test-message";
    Acknowledgment acknowledgment = mock(Acknowledgment.class);

    Message<String> message = MessageBuilder.withPayload(payload)
      .setHeader("kafka_acknowledgment", acknowledgment)
      .build();


    consumer.execute(message);

    verify(acknowledgment).acknowledge();
  }




}
