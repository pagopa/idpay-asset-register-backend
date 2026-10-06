package it.gov.pagopa.register.controller.clean;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import it.gov.pagopa.register.service.clean.FixtureCleanupService;
import it.gov.pagopa.register.service.role.PortalConsentService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FixtureCleanupConditionalTest {
  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withBean(ProductFileRepository.class, () -> mock(ProductFileRepository.class))
      .withBean(ProductRepository.class, () -> mock(ProductRepository.class))
      .withBean(ProducersInitiativeRepository.class, () -> mock(ProducersInitiativeRepository.class))
      .withBean(FileStorageClient.class, () -> mock(FileStorageClient.class))
      .withBean(MongoTemplate.class, () -> mock(MongoTemplate.class))
      .withBean(PortalConsentService.class, () -> mock(PortalConsentService.class))
      .withUserConfiguration(FixtureCleanupController.class, FixtureCleanupService.class);

  @Test
  void cleanupIsAbsentWhenPropertyIsMissing() {
    runner.run(context -> {
      assertThat(context).doesNotHaveBean(FixtureCleanupController.class);
      assertThat(context).doesNotHaveBean(FixtureCleanupService.class);
    });
  }

  @Test
  void cleanupIsAbsentWhenTestSupportIsDisabled() {
    runner.withPropertyValues("app.test-support.enabled=false").run(context -> {
      assertThat(context).doesNotHaveBean(FixtureCleanupController.class);
      assertThat(context).doesNotHaveBean(FixtureCleanupService.class);
    });
  }

  @Test
  void cleanupIsAvailableWhenTestSupportIsEnabled() {
    runner.withPropertyValues("app.test-support.enabled=true").run(context -> {
      assertThat(context).hasSingleBean(FixtureCleanupController.class);
      assertThat(context).hasSingleBean(FixtureCleanupService.class);
    });
  }
}
