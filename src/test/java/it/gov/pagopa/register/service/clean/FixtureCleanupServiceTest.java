package it.gov.pagopa.register.service.clean;

import com.azure.storage.blob.models.BlobStorageException;
import com.mongodb.client.result.DeleteResult;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.controller.clean.FixtureCleanupController.AssociationRestore;
import it.gov.pagopa.register.controller.clean.FixtureCleanupController.FileScope;
import it.gov.pagopa.register.controller.clean.FixtureCleanupController.ProductScope;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixtureCleanupServiceTest {
  private static final String ORGANIZATION = "83843864-f3c0-4def-badb-7f197471b72e";
  private static final String INITIATIVE = "65c3b1e3e4b0a1a2b3c4d5e6";
  private static final String FILE_ID = "65c3b1e3e4b0a1a2b3c4d5e7";
  private static final String ASSOCIATION_ID = ORGANIZATION + "_" + INITIATIVE;
  private static final FileScope SCOPE = new FileScope(ORGANIZATION, INITIATIVE, "unique.csv");

  @Mock private ProductFileRepository files;
  @Mock private ProductRepository products;
  @Mock private ProducersInitiativeRepository associations;
  @Mock private FileStorageClient storage;
  @Mock private MongoTemplate mongo;
  @Mock private MongoConverter converter;
  @InjectMocks private FixtureCleanupService service;

  private ProductFile file(String status) {
    return ProductFile.builder().id(FILE_ID).organizationId(ORGANIZATION)
        .initiativeId(INITIATIVE).organizationName("Fixture organization").category("COOKINGHOBS")
        .fileName("unique.csv").uploadStatus(status).build();
  }

  private void selected(ProductFile file) {
    when(files.findByOrganizationIdAndInitiativeIdAndFileName(ORGANIZATION, INITIATIVE, "unique.csv"))
        .thenReturn(List.of(file));
  }

  private ProducersInitiative association(String email) {
    return ProducersInitiative.builder().id(ASSOCIATION_ID).producerId(ORGANIZATION)
        .initiativeId(INITIATIVE).producerEmail(email).enabled(true)
        .updatedAt(LocalDateTime.of(2026, 10, 5, 12, 0)).build();
  }

  private void serializeExpected() {
    when(mongo.getConverter()).thenReturn(converter);
    doAnswer(invocation -> {
      ProducersInitiative value = invocation.getArgument(0);
      Document document = invocation.getArgument(1);
      document.put("_id", value.getId());
      document.put("producerEmail", value.getProducerEmail());
      document.put("updatedAt", value.getUpdatedAt());
      return null;
    }).when(converter).write(any(ProducersInitiative.class), any(Document.class));
  }

  @Test
  void deletesOnlyMatchedFileItsProductsAndCorrectStoragePaths() {
    selected(file("PARTIAL"));
    Product owned = Product.builder().id("product-id").organizationId(ORGANIZATION)
        .initiativeId(INITIATIVE).productFileId(FILE_ID).build();
    when(products.findByProductFileId(FILE_ID)).thenReturn(List.of(owned));
    assertTrue(service.deleteFiles(SCOPE));
    var ordered = inOrder(storage, products, files);
    ordered.verify(storage).deleteFile("Report/Partial/" + INITIATIVE + "/" + FILE_ID + ".csv");
    ordered.verify(storage).deleteFile("Report/Formal/" + INITIATIVE + "/" + FILE_ID + ".csv");
    ordered.verify(storage).deleteFile("CSV/" + INITIATIVE + "/" + ORGANIZATION
        + "/Fixture organization/COOKINGHOBS/" + FILE_ID + ".csv");
    ordered.verify(products).deleteAll(List.of(owned));
    ordered.verify(files).deleteById(FILE_ID);
    verify(files, never()).deleteAll();
    verify(products, never()).deleteAll();
  }

  @ParameterizedTest
  @ValueSource(strings = {"UPLOADED", "IN_PROCESS"})
  void activeUploadCannotBeDeleted(String status) {
    selected(file(status));
    assertFalse(service.deleteFiles(SCOPE));
    verifyNoInteractions(products, storage);
    verify(files, never()).deleteById(any());
  }

  @Test
  void alreadyRemovedFileIsSuccessfulNoOp() {
    when(files.findByOrganizationIdAndInitiativeIdAndFileName(ORGANIZATION, INITIATIVE, "unique.csv"))
        .thenReturn(List.of());
    assertTrue(service.deleteFiles(SCOPE));
    verifyNoInteractions(products, storage);
  }

  @Test
  void productInDifferentOrganizationBlocksDeletion() {
    selected(file("LOADED"));
    when(products.findByProductFileId(FILE_ID)).thenReturn(List.of(
        Product.builder().organizationId("foreign").initiativeId(INITIATIVE).build()));
    assertFalse(service.deleteFiles(SCOPE));
    verifyNoInteractions(storage);
    verify(products, never()).deleteAll(anyList());
    verify(files, never()).deleteById(any());
  }

  @Test
  void storageFailureLeavesDatabaseRecordsUntouched() {
    selected(file("FORMAL_ERROR"));
    when(products.findByProductFileId(FILE_ID)).thenReturn(List.of());
    when(storage.deleteFile(any())).thenThrow(new IllegalStateException("Storage unavailable"));
    assertThrows(IllegalStateException.class, () -> service.deleteFiles(SCOPE));
    verify(products, never()).deleteAll(anyList());
    verify(files, never()).deleteById(any());
  }

  @ParameterizedTest
  @ValueSource(ints = {403, 500})
  void storageAuthorizationAndServerErrorsAreNotIgnored(int status) {
    selected(file("FORMAL_ERROR"));
    when(products.findByProductFileId(FILE_ID)).thenReturn(List.of());
    BlobStorageException error = mock(BlobStorageException.class);
    when(error.getStatusCode()).thenReturn(status);
    when(storage.deleteFile(any())).thenThrow(error);
    assertThrows(BlobStorageException.class, () -> service.deleteFiles(SCOPE));
    verify(files, never()).deleteById(any());
  }

  @Test
  void missingBlobDoesNotPreventRecordDeletion() {
    selected(file("FORMAL_ERROR"));
    when(products.findByProductFileId(FILE_ID)).thenReturn(List.of());
    BlobStorageException missing = mock(BlobStorageException.class);
    when(missing.getStatusCode()).thenReturn(404);
    when(storage.deleteFile(any())).thenThrow(missing);
    assertTrue(service.deleteFiles(SCOPE));
    verify(files).deleteById(FILE_ID);
  }

  @Test
  void productLookupUsesExactGtinsAndInitiative() {
    List<String> gtins = List.of("ABC", "DEF");
    service.products(new ProductScope(INITIATIVE, gtins));
    verify(products).findByGtinCodeInAndInitiativeId(gtins, INITIATIVE);
  }

  @Test
  void unchangedSnapshotDoesNotRewriteAssociation() {
    ProducersInitiative before = association("original");
    when(associations.findById(ASSOCIATION_ID)).thenReturn(Optional.of(before));
    assertTrue(service.restore(new AssociationRestore(ASSOCIATION_ID, before, before)));
    verifyNoInteractions(mongo);
  }

  @Test
  void concurrentChangeCannotBeOverwritten() {
    when(associations.findById(ASSOCIATION_ID)).thenReturn(Optional.of(association("external")));
    assertFalse(service.restore(new AssociationRestore(ASSOCIATION_ID,
        association("original"), association("test"))));
    verifyNoInteractions(mongo);
  }

  @Test
  void mismatchedSnapshotIdIsRejectedBeforeRepositoryAccess() {
    AssociationRestore request = new AssociationRestore("other-id", association("original"), association("test"));
    assertThrows(ClientExceptionNoBody.class, () -> service.restore(request));
    verifyNoInteractions(associations, mongo);
  }

  @Test
  void existingAssociationIsRestoredWithAtomicComparisonIncludingNullFields() {
    ProducersInitiative before = association("original");
    ProducersInitiative expected = association(null);
    when(associations.findById(ASSOCIATION_ID)).thenReturn(Optional.of(expected));
    serializeExpected();
    when(mongo.findAndReplace(any(Query.class), eq(before))).thenReturn(expected);
    assertTrue(service.restore(new AssociationRestore(ASSOCIATION_ID, before, expected)));
    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongo).findAndReplace(query.capture(), eq(before));
    assertEquals(ASSOCIATION_ID, query.getValue().getQueryObject().get("_id"));
    assertTrue(query.getValue().getQueryObject().containsKey("producerEmail"));
    assertNull(query.getValue().getQueryObject().get("producerEmail"));
    assertEquals(expected.getUpdatedAt(), query.getValue().getQueryObject().get("updatedAt"));
    verify(associations, never()).save(any());
  }

  @Test
  void raceDuringAtomicRestoreReturnsConflict() {
    ProducersInitiative before = association("original");
    ProducersInitiative expected = association("test");
    when(associations.findById(ASSOCIATION_ID)).thenReturn(Optional.of(expected));
    serializeExpected();
    assertFalse(service.restore(new AssociationRestore(ASSOCIATION_ID, before, expected)));
  }

  @Test
  void newAssociationIsRemovedByExactAtomicPredicate() {
    ProducersInitiative expected = association("test");
    when(associations.findById(ASSOCIATION_ID)).thenReturn(Optional.of(expected));
    serializeExpected();
    when(mongo.remove(any(Query.class), eq(ProducersInitiative.class))).thenReturn(DeleteResult.acknowledged(1));
    assertTrue(service.restore(new AssociationRestore(ASSOCIATION_ID, null, expected)));
    verify(associations, never()).deleteById(any());
  }

  @Test
  void raceDuringAtomicDeletionReturnsConflict() {
    ProducersInitiative expected = association("test");
    when(associations.findById(ASSOCIATION_ID)).thenReturn(Optional.of(expected));
    serializeExpected();
    when(mongo.remove(any(Query.class), eq(ProducersInitiative.class))).thenReturn(DeleteResult.acknowledged(0));
    assertFalse(service.restore(new AssociationRestore(ASSOCIATION_ID, null, expected)));
  }
}
