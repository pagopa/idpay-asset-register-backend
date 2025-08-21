package it.gov.pagopa.register.utils;

import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;


import static it.gov.pagopa.register.constants.AssetRegisterConstants.USERNAME;

public class ObjectMaker {

  private ObjectMaker(){}

  public static  ArrayList<StatusChangeEvent> buildStatusChangeEventsList(){
    ArrayList<StatusChangeEvent> arrayList = new ArrayList<>();

    arrayList.add(StatusChangeEvent.builder()
      .username(USERNAME)
      .role("L1")
      .updateDate(LocalDateTime.now())
      .currentStatus(ProductStatus.UPLOADED)
      .targetStatus(ProductStatus.APPROVED)
      .motivation("Test")
      .build());

    return arrayList;
  }
}
