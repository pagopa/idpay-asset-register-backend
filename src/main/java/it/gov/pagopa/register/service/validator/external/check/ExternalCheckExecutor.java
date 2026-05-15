package it.gov.pagopa.register.service.validator.external.check;

import it.gov.pagopa.register.configuration.initiative.model.ExternalCheckTemplate;
import it.gov.pagopa.register.configuration.initiative.model.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.external.ExternalRuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleDispatcher;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import it.gov.pagopa.register.service.validator.external.system.ExternalSystemClient;
import it.gov.pagopa.register.service.validator.external.system.ExternalSystemClientDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.ERROR_MAP;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalCheckExecutor {

  private final ExternalSystemClientDispatcher systemClientDispatcher;
  private final RuleDispatcher ruleDispatcher;


  public ExternalCheckResult execute(
      CSVRecord csvRecord,
      ExternalCheckTemplate template,
      Map<String, Object> categoryParameters,
      String category
  ) {

    // Risolvi il client del sistema esterno (EPREL, altro)
    ExternalSystemClient systemClient =
        systemClientDispatcher.resolve(template.getType());

    if (systemClient == null) {
      return ExternalCheckResult.ko(
          "Sistema esterno non supportato: " + template.getType()
      );
    }

    // Chiamata al sistema esterno
    Map<String, Object> externalData;
    try {
      externalData = systemClient.fetch(csvRecord, template);
    } catch (Exception ex) {
      log.error(
          "[EXTERNAL_CHECK] Errore durante la chiamata al sistema {}",
          template.getType(),
          ex
      );
      return ExternalCheckResult.ko("Errore durante la chiamata " + template.getType());
    }

    if (externalData == null || externalData.isEmpty()) {
      return ExternalCheckResult.ko(template.getType() + " non ha restituito dati validi");
    }

    // Applica le regole dichiarative
    List<ValidationRule> rules = template.getRules();

    if (rules != null) {
      for (ValidationRule rule : rules) {

        RuleExecutor ruleExecutor =
            ruleDispatcher.resolve(rule.getKey());

        if (ruleExecutor == null) {
          return ExternalCheckResult.ko(
            "Errore nella configurazione per il recupero dei dati"
          );
        }

        boolean valid = ruleExecutor.evaluate(
            rule,
            new ExternalRuleContext(externalData,categoryParameters,category)
        );

        if (!valid) {
          return ExternalCheckResult.ko(
            ERROR_MAP.get(rule.getErrorKey().replace("{}", category))
          );
        }
      }
    }

    // Tutte le validazioni superate
    return ExternalCheckResult.ok(externalData);
  }
}
