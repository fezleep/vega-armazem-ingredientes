package br.com.vega.armazem.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Ingrediente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String nomeNormalizado;
    @Enumerated(EnumType.STRING)
    private Tipo tipo;
    private BigDecimal quantidade;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Compartimento compartimento;
    private Instant criadoEm;
    private Instant atualizadoEm;

    protected Ingrediente() {}

    public Ingrediente(String nome, String nomeNormalizado, Tipo tipo, BigDecimal quantidade,
                       Compartimento compartimento, Instant data) {
        this.nome = nome;
        this.nomeNormalizado = nomeNormalizado;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.compartimento = compartimento;
        this.criadoEm = data;
        this.atualizadoEm = data;
    }

    public void atualizarSaldo(BigDecimal saldo, Instant data) {
        quantidade = saldo;
        atualizadoEm = data;
    }

    public Long id() { return id; }
    public String nome() { return nome; }
    public Tipo tipo() { return tipo; }
    public BigDecimal quantidade() { return quantidade; }
    public Compartimento compartimento() { return compartimento; }
    public Instant criadoEm() { return criadoEm; }
    public Instant atualizadoEm() { return atualizadoEm; }
}
