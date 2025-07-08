package it.gov.pagopa.register.utils;

import lombok.Getter;

@Getter
public class EventDetails {
  private final String orgId;
  private final String category;
  private final String productFileId;

  public EventDetails(String orgId, String category, String productFileId) {
    this.orgId = orgId;
    this.category = category;
    this.productFileId = productFileId;
  }
}
