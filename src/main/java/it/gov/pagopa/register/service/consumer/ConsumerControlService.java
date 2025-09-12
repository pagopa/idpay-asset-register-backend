package it.gov.pagopa.register.service.consumer;


import it.gov.pagopa.register.connector.eprel.EprelConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConsumerControlService {

  public static final String PRODUCT_FILE_CONSUMER_IN_0 = "productFileConsumer-in-0";
  private final BindingsLifecycleController lifecycleController;
  private final EprelConnector eprelConnector;

  public ConsumerControlService(BindingsLifecycleController lifecycleController, EprelConnector eprelConnector) {
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
    log.info("[EPREL_HEALTH] - Starting loop to wait for EPREL...");

    boolean shouldContinue = true;

    while (shouldContinue) {
      try {
        eprelConnector.callEprel("2310908");
        log.info("[EPREL_HEALTH] - EPREL is available. Restarting Kafka consumer.");
        startConsumer();
        shouldContinue = false;
      } catch (Exception e) {
        log.warn("[EPREL_HEALTH] - EPREL still unavailable. Retrying in 10 seconds...");
        try {
          Thread.sleep(10_000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.error("[EPREL_HEALTH] - Thread interrupted. Exiting health check.");
          startConsumer();
          shouldContinue = false;
        }
      }
    }
  }

}


