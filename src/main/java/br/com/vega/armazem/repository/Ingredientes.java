package br.com.vega.armazem.repository;

import br.com.vega.armazem.domain.Ingrediente;
import br.com.vega.armazem.domain.Tipo;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface Ingredientes extends JpaRepository<Ingrediente, Long> {
    @Query("select i.compartimento.id from Ingrediente i where i.id = :id")
    Optional<Long> acharCompartimentoId(@Param("id") Long id);

    boolean existsByNomeNormalizadoAndTipoAndCompartimentoId(String nome, Tipo tipo, Long compartimentoId);

    List<Ingrediente> findByCompartimentoIdAndQuantidadeGreaterThan(Long id, java.math.BigDecimal quantidade);

    @EntityGraph(attributePaths = "compartimento")
    List<Ingrediente> findAllByOrderByIdAsc();
}
