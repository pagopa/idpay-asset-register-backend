package it.gov.pagopa.register.dto.utils;

import lombok.Getter;

@Getter
public class EventDetails {
  private final String orgId;
  private final String category;
  private final String productFileId;
  private final String organizationName;
  private final String initiativeId;
  public EventDetails(String orgId, String category, String productFileId, String organizationName, String initiativeId) {
    this.orgId = orgId;
    this.category = category;
    this.productFileId = productFileId;
    this.organizationName = organizationName;
    this.initiativeId = initiativeId;
  }
}
