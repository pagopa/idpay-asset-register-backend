package it.gov.pagopa.common.kafka.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.kafka.BaseKafkaConsumer;
import org.springframework.messaging.Message;


public class BaseConsumer extends BaseKafkaConsumer {


  protected BaseConsumer(String applicationName) {
    super(applicationName);
  }

  @Override
  public void onError(Message message, Throwable e) {
//void
  }

  @Override
  public ObjectReader getObjectReader() {
    return new ObjectMapper()
      .readerFor(String.class);
  }

  @Override
  public void execute(Object payload, Message message) {
//void
  }

  @Override
  public void onDeserializationError(Message message, Throwable e) {
//void
  }
}
