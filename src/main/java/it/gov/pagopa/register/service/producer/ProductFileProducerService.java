package it.gov.pagopa.register.service.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductFileProducerService {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final String topic;

  public ProductFileProducerService(KafkaTemplate<String, Object> kafkaTemplate,
                                    @Value("${spring.cloud.stream.bindings.productFileConsumer-in-0.destination}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  public void sendMessage(Object payload) {
    kafkaTemplate.send(topic, payload);
  }

}
