package it.gov.pagopa.register.dto.operation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageEventDTO {
    private String id;
    private String subject;
    private String eventType;
    private StorageEventData data;
    private LocalDateTime eventTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StorageEventData {
        private String eTag;
        private Integer contentLength;
        private String url;
    }
}
