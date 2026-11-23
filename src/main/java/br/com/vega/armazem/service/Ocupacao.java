package br.com.vega.armazem.service;

import br.com.vega.armazem.domain.Compartimento;
import br.com.vega.armazem.domain.Ingrediente;
import br.com.vega.armazem.domain.Tipo;
import br.com.vega.armazem.dto.Contratos.Estado;
import br.com.vega.armazem.exception.ErroDeNegocio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

record Ocupacao(Compartimento compartimento, Tipo tipoAtual, BigDecimal total) {
    static Ocupacao calcular(Compartimento compartimento, List<Ingrediente> positivos) {
        var tipo = positivos.isEmpty() ? null : positivos.getFirst().tipo();
        var total = positivos.stream().map(Ingrediente::quantidade).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Ocupacao(compartimento, tipo, total);
    }

    void validarEntrada(Tipo tipo, BigDecimal quantidade, LocalDate hoje) {
        if (tipoAtual != null && tipoAtual != tipo) {
            throw ErroDeNegocio.invalido("O compartimento já contém ingredientes de outra categoria.");
        }
        if (bloqueado(hoje) && compartimento.ultimoTipo() != tipo) {
            throw ErroDeNegocio.invalido("O compartimento só pode mudar de categoria no próximo dia civil.");
        }
        if (total.add(quantidade).compareTo(tipo.capacidade()) > 0) {
            throw ErroDeNegocio.invalido("Quantidade excede o espaço livre do compartimento.");
        }
    }

    boolean comporta(Tipo tipo, BigDecimal quantidade, LocalDate hoje) {
        return (tipoAtual == null || tipoAtual == tipo)
            && (!bloqueado(hoje) || compartimento.ultimoTipo() == tipo)
            && total.add(quantidade).compareTo(tipo.capacidade()) <= 0;
    }

    private boolean bloqueado(LocalDate hoje) {
        return tipoAtual == null && compartimento.possuiRestricao(hoje);
    }

    Estado estado(Tipo alternativa, LocalDate hoje) {
        boolean bloqueado = bloqueado(hoje);
        Tipo aplicavel = tipoAtual;
        if (aplicavel == null) {
            aplicavel = bloqueado ? compartimento.ultimoTipo() : alternativa;
        }
        var capacidade = aplicavel == null ? null : aplicavel.capacidade();
        var livre = capacidade == null ? null : capacidade.subtract(total);
        return new Estado(compartimento.id(), compartimento.codigo(), tipoAtual, compartimento.ultimoTipo(),
            compartimento.ultimaData(), total, capacidade, livre, aplicavel == null ? null : aplicavel.unidade(),
            bloqueado, livre == null || livre.signum() > 0);
    }
}
