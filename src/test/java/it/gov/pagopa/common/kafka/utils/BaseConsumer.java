package it.gov.pagopa.common.kafka.utils;

import it.gov.pagopa.common.kafka.BaseKafkaConsumer;
import org.springframework.messaging.Message;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;


public class BaseConsumer extends BaseKafkaConsumer<String> {


  public BaseConsumer(String applicationName) {
    super(applicationName);
  }

  @Override
  protected void onError(Message<String> message, Throwable e) {
    //not tested
  }


  @Override
  public ObjectReader getObjectReader() {
    return new ObjectMapper()
      .readerFor(String.class);
  }

  @Override
  protected void onDeserializationError(Message<String> message, Throwable e) {
    //not tested
  }

  @Override
  public void execute(String payload, Message<String> message) {
    System.out.println(payload);
    if(payload.equals("exception"))
      throw new RuntimeException();
  }



}
