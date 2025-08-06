package it.gov.pagopa.register.enums;

import lombok.Getter;

@Getter
public enum UserRole {
  OPERATORE("operatore"),
  INVITALIA("invitalia"),
  INVITALIA_ADMIN("invitalia_admin");

  private final String role;

  UserRole(String label) {
    this.role = label;
  }

}
