package it.gov.pagopa.register.constants;

public final class ExceptionConstants {

    private ExceptionConstants(){}

    public static final class ExceptionCode {
        public static final String PERMISSIONS_NOT_FOUND = "NOT_FOUND";
        public static final String CONSENT_NOT_FOUND = "CONSENT_NOT_FOUND";
        public static final String VERSION_NOT_MATCHED = "VERSION_NOT_MATCHED";
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";
        public static final String INVALID_REQUEST = "INVALID_REQUEST";
        public static final String REPORT_NOT_FOUND = "REPORT_NOT_FOUND";
        public static final String EPREL_EXCEPTION = "EPREL_EXCEPTION";

        private ExceptionCode() {}
    }

    public static final class ExceptionMessage {
      public static final String PERMISSIONS_NOT_FOUND_MSG = "Permissions not found for [%s] role";
      private ExceptionMessage() {}
    }
}
