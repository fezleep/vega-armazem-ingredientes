package br.com.vega.armazem.repository;

import br.com.vega.armazem.domain.Movimentacao;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;
import java.util.List;

public interface Movimentacoes extends Repository<Movimentacao, Long> {
    Movimentacao save(Movimentacao movimento);
    List<Movimentacao> findAll(Sort sort);
}
