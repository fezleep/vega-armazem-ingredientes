package br.com.vega.armazem.dto;

import br.com.vega.armazem.domain.Operacao;
import br.com.vega.armazem.domain.Tipo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class Contratos {
    private Contratos() {}

    public record Cadastro(@NotBlank @Size(max = 120) String nome,
        @NotNull @Schema(description = "Categoria. O alias SECOS também é aceito.", example = "SECOS") Tipo tipo,
        @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantidade,
        @NotNull @Positive Long compartimentoId,
        @NotBlank @Size(max = 120) String responsavel) {}

    public record Ajuste(@NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantidade,
        @NotBlank @Size(max = 120) String responsavel) {}

    public record Estoque(long id, String nome, Tipo tipo, BigDecimal quantidade, String unidade,
        long compartimentoId, String compartimentoCodigo, Instant criadoEm, Instant atualizadoEm) {}

    public record Saldo(long ingredienteId, BigDecimal quantidade, long movimentacaoId) {}

    public record Volume(Tipo type, BigDecimal totalQuantity) {}

    public record Estado(long id, String codigo, Tipo tipoAtual, Tipo ultimoTipoUtilizado,
        LocalDate dataUltimaUtilizacao, BigDecimal quantidadeTotal, BigDecimal capacidade,
        BigDecimal espacoLivre, String unidade, boolean bloqueadoParaOutraCategoria, boolean disponivel) {}

    public record Evento(long id, Instant data, Operacao operacao, BigDecimal quantidade, Tipo tipo,
        long ingredienteId, String ingredienteNome, long compartimentoId, String compartimentoCodigo,
        String responsavel) {}
}
