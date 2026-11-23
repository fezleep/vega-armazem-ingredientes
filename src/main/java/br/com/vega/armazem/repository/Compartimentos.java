package br.com.vega.armazem.repository;

import br.com.vega.armazem.domain.Compartimento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface Compartimentos extends JpaRepository<Compartimento, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Compartimento c where c.id = :id")
    Optional<Compartimento> bloquear(@Param("id") Long id);
}
