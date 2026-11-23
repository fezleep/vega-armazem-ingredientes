package br.com.vega.armazem.service;

import br.com.vega.armazem.domain.Compartimento;
import br.com.vega.armazem.domain.Tipo;
import br.com.vega.armazem.exception.ErroDeNegocio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OcupacaoTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 1, 1);

    @Test
    void mostraCapacidadeReservadaNoDiaDaUltimaUtilizacao() {
        var compartimento = mock(Compartimento.class);
        when(compartimento.id()).thenReturn(1L);
        when(compartimento.ultimoTipo()).thenReturn(Tipo.REFRIGERADO);
        when(compartimento.possuiRestricao(HOJE)).thenReturn(true);
        var ocupacao = new Ocupacao(compartimento, null, BigDecimal.ZERO);

        assertThat(ocupacao.estado(null, HOJE).espacoLivre()).isEqualByComparingTo("400");
        assertThat(ocupacao.comporta(Tipo.REFRIGERADO, new BigDecimal("400"), HOJE)).isTrue();
        assertThatThrownBy(() -> ocupacao.validarEntrada(Tipo.LIQUIDO, BigDecimal.ONE, HOJE))
            .isInstanceOf(ErroDeNegocio.class).hasMessageContaining("próximo dia civil");
        assertThat(ocupacao.comporta(Tipo.LIQUIDO, new BigDecimal("500"), HOJE.plusDays(1))).isTrue();
    }

    @Test
    void projetaEspacoLivreConformeCategoriaSolicitada() {
        var compartimento = mock(Compartimento.class);
        when(compartimento.id()).thenReturn(1L);
        var ocupacao = new Ocupacao(compartimento, null, BigDecimal.ZERO);
        assertThat(ocupacao.estado(null, HOJE).capacidade()).isNull();
        assertThat(ocupacao.estado(Tipo.LIQUIDO, HOJE).espacoLivre()).isEqualByComparingTo("500");
        assertThat(ocupacao.estado(Tipo.LIQUIDO, HOJE).unidade()).isEqualTo("L");
    }
}
