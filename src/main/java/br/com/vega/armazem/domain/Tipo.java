package br.com.vega.armazem.domain;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAlias;

public enum Tipo {
    @JsonAlias("SECOS") SECO("600", "kg"),
    LIQUIDO("500", "L"),
    REFRIGERADO("400", "kg");

    private final BigDecimal capacidade;
    private final String unidade;

    Tipo(String capacidade, String unidade) {
        this.capacidade = new BigDecimal(capacidade);
        this.unidade = unidade;
    }

    public BigDecimal capacidade() { return capacidade; }
    public String unidade() { return unidade; }
}
