package it.gov.pagopa.register.event.consumer;

import it.gov.pagopa.register.service.consumer.ProductFileConsumerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class ProductFileConsumerConfig {

  @Bean
  public Consumer<Message<String>> productFileConsumer(ProductFileConsumerService productFileConsumerService) {
    return productFileConsumerService::execute;
  }
}
