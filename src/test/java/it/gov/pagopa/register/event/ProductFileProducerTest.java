package it.gov.pagopa.register.event;


import it.gov.pagopa.register.event.producer.ProductFileProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import static org.mockito.Mockito.*;


@ExtendWith({MockitoExtension.class})
class ProductFileProducerTest {

    @Mock
    private StreamBridge streamBridge;
    @InjectMocks
    private ProductFileProducer messageProducer;

    @Test
     void testStreamBridgeSendCalled() {
        messageProducer.scheduleMessage("test");
        verify(streamBridge, times(1)).send(eq("productFileProducer-out-0"), any(), eq("test"));
    }
}

