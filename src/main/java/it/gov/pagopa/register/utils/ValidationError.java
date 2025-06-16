package it.gov.pagopa.register.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {
    private int rowNumber;
    private String fieldName;
    private String errorMessage;
}


