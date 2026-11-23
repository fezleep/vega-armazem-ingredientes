package br.com.vega.armazem.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Immutable
public class Movimentacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant data;
    @Enumerated(EnumType.STRING)
    private Operacao operacao;
    private BigDecimal quantidade;
    @Enumerated(EnumType.STRING)
    private Tipo tipo;
    private Long ingredienteId;
    private String ingredienteNome;
    private Long compartimentoId;
    private String compartimentoCodigo;
    private String responsavel;

    protected Movimentacao() {}

    public Movimentacao(Ingrediente ingrediente, Operacao operacao, BigDecimal quantidade,
                        String responsavel, Instant data) {
        this.data = data;
        this.operacao = operacao;
        this.quantidade = quantidade;
        this.tipo = ingrediente.tipo();
        this.ingredienteId = ingrediente.id();
        this.ingredienteNome = ingrediente.nome();
        this.compartimentoId = ingrediente.compartimento().id();
        this.compartimentoCodigo = ingrediente.compartimento().codigo();
        this.responsavel = responsavel;
    }

    public Long id() { return id; }
    public Instant data() { return data; }
    public Operacao operacao() { return operacao; }
    public BigDecimal quantidade() { return quantidade; }
    public Tipo tipo() { return tipo; }
    public Long ingredienteId() { return ingredienteId; }
    public String ingredienteNome() { return ingredienteNome; }
    public Long compartimentoId() { return compartimentoId; }
    public String compartimentoCodigo() { return compartimentoCodigo; }
    public String responsavel() { return responsavel; }
}
