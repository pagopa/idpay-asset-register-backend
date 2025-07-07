package it.gov.pagopa.register.utils;

import lombok.Getter;

@Getter
public class EventDetails {
  private final String orgId;
  private final String category;
  private final String fileName;

  public EventDetails(String orgId, String category, String fileName) {
    this.orgId = orgId;
    this.category = category;
    this.fileName = fileName;
  }
}
