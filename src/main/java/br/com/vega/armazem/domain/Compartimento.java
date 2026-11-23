package br.com.vega.armazem.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Compartimento {
    @Id
    private Long id;
    private String codigo;
    @Enumerated(EnumType.STRING)
    private Tipo ultimoTipo;
    private LocalDate ultimaData;

    protected Compartimento() {}

    public Long id() { return id; }
    public String codigo() { return codigo; }
    public Tipo ultimoTipo() { return ultimoTipo; }
    public LocalDate ultimaData() { return ultimaData; }

    public boolean possuiRestricao(LocalDate hoje) {
        return ultimaData != null && !hoje.isAfter(ultimaData);
    }

    public void registrarUso(Tipo tipo, LocalDate data) {
        ultimoTipo = tipo;
        ultimaData = data;
    }
}
