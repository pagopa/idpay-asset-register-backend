package it.gov.pagopa.register.utils;

import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class Utils {


  // CSV REGEX
  public static final String CODICE_GTIN_EAN_REGEX = "^[a-zA-Z0-9]{1,14}$";
  public static final String CATEGORIA_REGEX = "[A-Za-z ]+";
  public static final String MARCA_REGEX = "^.{1,100}$";
  public static final String MODELLO_REGEX = "^.{1,100}$";
  public static final String CODICE_PRODOTTO_REGEX = "^[a-zA-Z0-9 ]{0,100}$";
  public static final String PAESE_DI_PRODUZIONE_REGEX = "^.{2}$";
  public static final String CODICE_EPREL_REGEX = "^\\d+$";


  // CSV HEADER
  public static final List<String> CSV_HEADER_PRODOTTI = List.of(
    "Codice EPREL",
    "Codice GTIN/EAN",
    "Codice Prodotto",
    "Categoria",
    "Paese di Produzione"
  );

  public static final List<String> CSV_HEADER_PIANI_COTTURA = List.of(
    "Codice GTIN/EAN",
    "Codice Prodotto",
    "Categoria",
    "Paese di Produzione",
    "Marca",
    "Modello"
  );


  public static final List<String> CATEGORIE_PRODOTTI = List.of(
    "WASHINGMACHINES",
    "WASHERDRIERS",
    "OVENS",
    "RANGEHOODS",
    "DISHWASHERS",
    "TUMBLEDRIERS",
    "REFRIGERATINGAPPL"
  );

  public static final List<String> CATEGORIE_PIANI_COTTURA = List.of(
    "COOKINGHOBS"
  );

}
