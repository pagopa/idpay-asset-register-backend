package it.gov.pagopa.register.event.producer;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class ProductFileProducer {

  private final String binder;
  private final StreamBridge streamBridge;

  public ProductFileProducer(StreamBridge streamBridge,
                             @Value("${spring.cloud.stream.bindings.productFileProducer-out-0.binder}")String binder) {
    this.streamBridge = streamBridge;
    this.binder = binder;
  }

  public boolean scheduleMessage(String payload) {
    log.info("[PRODUCT-FILE-PRODUCER] Sending message to binder '{}' with payload: {}", binder, payload);
    boolean sent = streamBridge.send("productFileProducer-out-0", binder, payload);
    if (sent) {
      log.info("[PRODUCT-FILE-PRODUCER] Message sent successfully.");
    } else {
      log.warn("[PRODUCT-FILE-PRODUCER] Failed to send message.");
    }
    return sent;
  }
}



