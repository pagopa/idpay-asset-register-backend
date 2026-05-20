package it.gov.pagopa.register.service.validator.external.system;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalSystemClientDispatcherTest {


  @Test
  void shouldResolveClientByType() {

    ExternalSystemClient client = mock(ExternalSystemClient.class);
    when(client.supports()).thenReturn("EPREL");

    ExternalSystemClientDispatcher dispatcher =
      new ExternalSystemClientDispatcher(List.of(client));

    ExternalSystemClient result = dispatcher.resolve("EPREL");

    assertNotNull(result);
    assertEquals(client, result);
  }


  @Test
  void shouldReturnNullWhenTypeNotFound() {

    ExternalSystemClient client = mock(ExternalSystemClient.class);
    when(client.supports()).thenReturn("EPREL");

    ExternalSystemClientDispatcher dispatcher =
      new ExternalSystemClientDispatcher(List.of(client));

    ExternalSystemClient result = dispatcher.resolve("UNKNOWN");

    assertNull(result);
  }

  @Test
  void shouldResolveCorrectClientAmongMultiple() {

    ExternalSystemClient client1 = mock(ExternalSystemClient.class);
    ExternalSystemClient client2 = mock(ExternalSystemClient.class);

    when(client1.supports()).thenReturn("EPREL");
    when(client2.supports()).thenReturn("ALTRO");

    ExternalSystemClientDispatcher dispatcher =
      new ExternalSystemClientDispatcher(List.of(client1, client2));

    ExternalSystemClient result = dispatcher.resolve("ALTRO");

    assertEquals(client2, result);
  }


  @Test
  void shouldThrowExceptionWhenDuplicateTypes() {
    ExternalSystemClient client1 = mock(ExternalSystemClient.class);
    ExternalSystemClient client2 = mock(ExternalSystemClient.class);

    when(client1.supports()).thenReturn("EPREL");
    when(client2.supports()).thenReturn("EPREL");

    List<ExternalSystemClient> clients = List.of(client1, client2);

    assertThrows(
      IllegalStateException.class,
      () -> new ExternalSystemClientDispatcher(clients)
    );
  }

  @Test
  void shouldReturnNullWhenNoClients() {

    ExternalSystemClientDispatcher dispatcher =
      new ExternalSystemClientDispatcher(List.of());

    ExternalSystemClient result = dispatcher.resolve("ANY");

    assertNull(result);
  }
}
