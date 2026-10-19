package it.gov.pagopa.register.service.clean;

import com.azure.storage.blob.models.BlobStorageException;
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
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.CSV;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.REPORT_FORMAL_ERROR;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.REPORT_PARTIAL_ERROR;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.test-support.enabled", havingValue = "true")
public class FixtureCleanupService {
  private static final Set<String> PROCESSING = Set.of("UPLOADED", "IN_PROCESS");
  private static final List<String> ASSOCIATION_FIELDS = List.of(
      "_id", "producerId", "producerName", "producerEmail", "initiativeId", "initiativeName",
      "initiativeStatus", "initiativeStartDate", "initiativeEndDate", "initiativeServiceId",
      "initiativeOrganizationName", "enabled", "source", "createdAt", "updatedAt");

  private final ProductFileRepository files;
  private final ProductRepository products;
  private final ProducersInitiativeRepository associations;
  private final FileStorageClient storage;
  private final MongoTemplate mongo;

  public List<Product> products(ProductScope scope) {
    return products.findByGtinCodeInAndInitiativeId(scope.gtinCodes(), scope.initiativeId());
  }

  public List<ProductFile> files(FileScope scope) {
    return files.findByOrganizationIdAndInitiativeIdAndFileName(
        scope.organizationId(), scope.initiativeId(), scope.fileName());
  }

  public boolean deleteFiles(FileScope scope) {
    List<ProductFile> selected = files(scope);
    if (selected.stream().anyMatch(file -> PROCESSING.contains(file.getUploadStatus()))) {
      return false;
    }
    for (ProductFile file : selected) {
      List<Product> storedProducts = products.findByProductFileId(file.getId());
      if (storedProducts.stream().anyMatch(product ->
          !Objects.equals(product.getOrganizationId(), scope.organizationId())
              || !Objects.equals(product.getInitiativeId(), scope.initiativeId()))) {
        return false;
      }
      // Keep the record until storage operations finish, so errors remain visible.
      deleteArtifacts(file);
      products.deleteAll(storedProducts);
      files.deleteById(file.getId());
    }
    return true;
  }

  private void deleteArtifacts(ProductFile file) {
    String suffix = file.getInitiativeId() + "/" + file.getId() + CSV;
    deleteBlob(REPORT_PARTIAL_ERROR + suffix);
    deleteBlob(REPORT_FORMAL_ERROR + suffix);
    deleteBlob(String.format("CSV/%s/%s/%s/%s/%s.csv", file.getInitiativeId(),
        file.getOrganizationId(), file.getOrganizationName(), file.getCategory(), file.getId()));
  }

  private void deleteBlob(String path) {
    try {
      storage.deleteFile(path);
    } catch (BlobStorageException error) {
      if (error.getStatusCode() != 404) {
        throw error;
      }
    }
  }

  public Optional<ProducersInitiative> association(String id) {
    return associations.findById(id);
  }

  public boolean restore(AssociationRestore request) {
    ProducersInitiative before = request.before();
    ProducersInitiative expected = request.expected();
    validateSnapshot(request.id(), before);
    validateSnapshot(request.id(), expected);
    ProducersInitiative current = associations.findById(request.id()).orElse(null);
    if (Objects.equals(current, before)) {
      return true;
    }
    if (expected == null || !Objects.equals(current, expected)) {
      return false;
    }
    Query query = snapshotQuery(expected);
    if (before == null) {
      return mongo.remove(query, ProducersInitiative.class).getDeletedCount() == 1;
    }
    return mongo.findAndReplace(query, before) != null;
  }

  private Query snapshotQuery(ProducersInitiative snapshot) {
    // Include null fields in the atomic predicate to detect concurrent changes too.
    Document document = new Document();
    mongo.getConverter().write(snapshot, document);
    Criteria criteria = new Criteria();
    ASSOCIATION_FIELDS.forEach(field -> criteria.and(field).is(document.get(field)));
    return new Query(criteria);
  }

  private void validateSnapshot(String id, ProducersInitiative snapshot) {
    if (snapshot != null && (!Objects.equals(snapshot.getId(), id)
        || !Objects.equals(snapshot.getProducerId() + "_" + snapshot.getInitiativeId(), id))) {
      throw new ClientExceptionNoBody(HttpStatus.BAD_REQUEST, "Association snapshot ID mismatch");
    }
  }
}
