package it.gov.pagopa.register.service.consumer;

import it.gov.pagopa.register.connector.eprel.EprelConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.web.client.HttpClientErrorException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumerControlServiceTest {

  @Mock
  private BindingsLifecycleController lifecycleController;

  @Mock
  private EprelConnector eprelConnector;

  @InjectMocks
  private ConsumerControlService consumerControlService;

  @Test
  void testStopConsumer() {
    consumerControlService.stopConsumer();

    verify(lifecycleController, times(1)).stop(ConsumerControlService.PRODUCT_FILE_CONSUMER_IN_0);
  }

  @Test
  void testStartConsumer() {
    consumerControlService.startConsumer();

    verify(lifecycleController, times(1)).start(ConsumerControlService.PRODUCT_FILE_CONSUMER_IN_0);
  }
  @Test
  void testStartEprelHealthCheck_HttpClientErrorException_StartsConsumer() {
    doThrow(new HttpClientErrorException(org.springframework.http.HttpStatus.BAD_REQUEST))
      .when(eprelConnector).callEprel("TEST");

    consumerControlService.startEprelHealthCheck();

    verify(lifecycleController, times(1)).start(ConsumerControlService.PRODUCT_FILE_CONSUMER_IN_0);
  }

  @Test
  void testStartEprelHealthCheck_UnexpectedException_StartsConsumer() {
    doThrow(new RuntimeException("Unexpected error"))
      .when(eprelConnector).callEprel("TEST");

    consumerControlService.startEprelHealthCheck();

    verify(lifecycleController, times(1)).start(ConsumerControlService.PRODUCT_FILE_CONSUMER_IN_0);
  }
}
