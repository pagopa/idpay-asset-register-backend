package it.gov.pagopa.register.service.consumer;


import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.exception.operation.EprelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.function.Supplier;

@Component
@Slf4j
public class ConsumerControlService {

  public static final String PRODUCT_FILE_CONSUMER_IN_0 = "productFileConsumer-in-0";
  private final BindingsLifecycleController lifecycleController;
  private final EprelConnector eprelConnector;

  public ConsumerControlService(
      BindingsLifecycleController lifecycleController,
      EprelConnector eprelConnector) {
    this.lifecycleController = lifecycleController;
    this.eprelConnector = eprelConnector;
  }

  public void stopConsumer() {
    lifecycleController.stop(PRODUCT_FILE_CONSUMER_IN_0);
  }

  public void startConsumer() {
    lifecycleController.start(PRODUCT_FILE_CONSUMER_IN_0);
  }


  public void startEprelHealthCheck() {
    log.info("[EPREL_HEALTH] - Starting retry");

    RetryConfig config = RetryConfig.custom()
      .maxAttempts(Integer.MAX_VALUE)
      .waitDuration(Duration.ofSeconds(10))
      .retryExceptions(EprelException.class)
      .build();

    Retry retry = Retry.of("eprelHealth", config);

    Supplier<Void> retryableCall = Retry.decorateSupplier(retry, () -> {
      try {
        eprelConnector.callEprel("TEST");
        log.info("[EPREL_HEALTH] - EPREL is available. Restarting Kafka consumer.");
        startConsumer();
      } catch (HttpClientErrorException e) {
        log.info("[EPREL_HEALTH] - EPREL returned client error {}, starting consumer anyway.", e.getStatusCode());
        startConsumer();
      } catch (HttpServerErrorException | ResourceAccessException e) {
        throw new EprelException("EPREL server error: " + e.getMessage());
      }
      return null;
    });


    try {
      retryableCall.get();
    } catch (Exception e) {
      log.error("[EPREL_HEALTH] - Unexpected error in health check: {}", e.getMessage());
      startConsumer();
    }
  }

}


