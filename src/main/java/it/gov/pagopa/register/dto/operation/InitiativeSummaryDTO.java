package it.gov.pagopa.register.dto.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitiativeSummaryDTO {

  @JsonProperty("initiativeId")
  private String initiativeId;

  @JsonProperty("initiativeName")
  private String initiativeName;

  @JsonProperty("initiativeRewardType")
  private String initiativeRewardType;

  @JsonProperty("status")
  private String status;

  @JsonProperty("serviceId")
  private String serviceId;

  @JsonProperty("organizationName")
  private String organizationName;

  @JsonProperty("creationDate")
  private LocalDateTime creationDate;

  @JsonProperty("updateDate")
  private LocalDateTime updateDate;

  @JsonProperty("startDate")
  private LocalDate startDate;
  @JsonProperty("endDate")
  private LocalDate endDate;

  @JsonProperty("rankingEnabled")
  private Boolean rankingEnabled;

}
