package br.com.vega.armazem.service;

import br.com.vega.armazem.domain.*;
import br.com.vega.armazem.dto.Contratos.*;
import br.com.vega.armazem.exception.ErroDeNegocio;
import br.com.vega.armazem.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static br.com.vega.armazem.config.ArmazemConfig.FUSO;

@Service
@Transactional(readOnly = true)
public class ArmazemService {
    private final Compartimentos compartimentos;
    private final Ingredientes ingredientes;
    private final Movimentacoes movimentacoes;
    private final Clock clock;

    public ArmazemService(Compartimentos compartimentos, Ingredientes ingredientes,
                          Movimentacoes movimentacoes, Clock clock) {
        this.compartimentos = compartimentos;
        this.ingredientes = ingredientes;
        this.movimentacoes = movimentacoes;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Estoque cadastrar(Cadastro cadastro) {
        validarQuantidade(cadastro.quantidade());
        String nome = texto(cadastro.nome(), "Nome");
        String responsavel = texto(cadastro.responsavel(), "Responsável");
        String chave = nome.toLowerCase(Locale.ROOT);
        var tipo = categoria(cadastro.tipo());
        var compartimento = bloquear(cadastro.compartimentoId());
        var instante = clock.instant();
        var hoje = dia(instante);

        if (ingredientes.existsByNomeNormalizadoAndTipoAndCompartimentoId(chave, tipo, compartimento.id())) {
            throw ErroDeNegocio.conflito("Ingrediente já cadastrado neste compartimento. Use a operação de entrada para repor.");
        }
        ocupacao(compartimento).validarEntrada(tipo, cadastro.quantidade(), hoje);
        var ingrediente = ingredientes.save(new Ingrediente(nome, chave, tipo, cadastro.quantidade(), compartimento, instante));
        compartimento.registrarUso(tipo, hoje);
        registrar(ingrediente, Operacao.ENTRADA, cadastro.quantidade(), responsavel, instante);
        return estoque(ingrediente);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Saldo movimentar(long id, Ajuste ajuste, Operacao operacao) {
        validarIdIngrediente(id);
        BigDecimal quantidade = ajuste.quantidade();
        validarQuantidade(quantidade);
        var responsavel = texto(ajuste.responsavel(), "Responsável");
        Long compartimentoId = ingredientes.acharCompartimentoId(id).orElseThrow(ArmazemService::ingredienteInexistente);
        var compartimento = bloquear(compartimentoId);
        var ingrediente = ingrediente(id);
        var instante = clock.instant();
        var hoje = dia(instante);
        BigDecimal novoSaldo;

        if (operacao == Operacao.ENTRADA) {
            ocupacao(compartimento).validarEntrada(ingrediente.tipo(), quantidade, hoje);
            novoSaldo = ingrediente.quantidade().add(quantidade);
        } else {
            novoSaldo = ingrediente.quantidade().subtract(quantidade);
            if (novoSaldo.signum() < 0) {
                throw ErroDeNegocio.invalido("Saldo insuficiente para retirar essa quantidade do ingrediente.");
            }
        }
        ingrediente.atualizarSaldo(novoSaldo, instante);
        compartimento.registrarUso(ingrediente.tipo(), hoje);
        var evento = registrar(ingrediente, operacao, quantidade, responsavel, instante);
        return new Saldo(id, novoSaldo, evento.id());
    }

    public List<Estoque> listarIngredientes() {
        return ingredientes.findAllByOrderByIdAsc().stream().map(ArmazemService::estoque).toList();
    }

    public Estoque consultarIngrediente(long id) {
        return estoque(ingrediente(id));
    }

    public List<Volume> volumes() {
        var totais = new EnumMap<Tipo, BigDecimal>(Tipo.class);
        ingredientes.findAll().forEach(i -> totais.merge(i.tipo(), i.quantidade(), BigDecimal::add));
        return Arrays.stream(Tipo.values()).map(tipo -> new Volume(tipo, totais.getOrDefault(tipo, BigDecimal.ZERO))).toList();
    }

    public List<Estado> estados() {
        var hoje = dia(clock.instant());
        return ocupacoes().stream().map(o -> o.estado(null, hoje)).toList();
    }

    public List<Estado> disponiveis(Tipo tipo, BigDecimal quantidade) {
        validarQuantidade(quantidade);
        categoria(tipo);
        var hoje = dia(clock.instant());
        return ocupacoes().stream().filter(o -> o.comporta(tipo, quantidade, hoje))
            .map(o -> o.estado(tipo, hoje)).toList();
    }

    public List<Estado> disponiveisParaVenda(Tipo tipo) {
        categoria(tipo);
        var hoje = dia(clock.instant());
        return ocupacoes().stream().filter(o -> o.tipoAtual() == tipo).map(o -> o.estado(tipo, hoje)).toList();
    }

    public List<Evento> historico(String sortBy, String order) {
        String[] campos = switch (sortBy) {
            case "date" -> new String[]{"data", "compartimentoId", "id"};
            case "compartimento" -> new String[]{"compartimentoId", "data", "id"};
            default -> throw ErroDeNegocio.invalido("sortBy deve ser date ou compartimento.");
        };
        var direcao = switch (order) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw ErroDeNegocio.invalido("order deve ser asc ou desc.");
        };
        return movimentacoes.findAll(Sort.by(direcao, campos)).stream()
            .map(m -> new Evento(m.id(), m.data(), m.operacao(), m.quantidade(), m.tipo(), m.ingredienteId(),
                m.ingredienteNome(), m.compartimentoId(), m.compartimentoCodigo(), m.responsavel())).toList();
    }

    private List<Ocupacao> ocupacoes() {
        var positivosPorCompartimento = ingredientes.findAllByOrderByIdAsc().stream()
            .filter(i -> i.quantidade().signum() > 0)
            .collect(Collectors.groupingBy(i -> i.compartimento().id()));
        return compartimentos.findAll(Sort.by("id")).stream()
            .map(c -> Ocupacao.calcular(c, positivosPorCompartimento.getOrDefault(c.id(), List.of()))).toList();
    }

    private Ocupacao ocupacao(Compartimento compartimento) {
        return Ocupacao.calcular(compartimento,
            ingredientes.findByCompartimentoIdAndQuantidadeGreaterThan(compartimento.id(), BigDecimal.ZERO));
    }

    private Compartimento bloquear(Long id) {
        if (id == null || id <= 0) {
            throw ErroDeNegocio.invalido("O ID do compartimento deve ser positivo.");
        }
        return compartimentos.bloquear(id).orElseThrow(() -> ErroDeNegocio.inexistente("Compartimento inexistente."));
    }

    private Ingrediente ingrediente(long id) {
        validarIdIngrediente(id);
        return ingredientes.findById(id).orElseThrow(ArmazemService::ingredienteInexistente);
    }

    private static void validarIdIngrediente(long id) {
        if (id <= 0) {
            throw ErroDeNegocio.invalido("O ID do ingrediente deve ser positivo.");
        }
    }

    private static ErroDeNegocio ingredienteInexistente() {
        return ErroDeNegocio.inexistente("Ingrediente inexistente.");
    }

    private Movimentacao registrar(Ingrediente ingrediente, Operacao operacao, BigDecimal quantidade,
                                   String responsavel, Instant instante) {
        return movimentacoes.save(new Movimentacao(ingrediente, operacao, quantidade, responsavel, instante));
    }

    private static Estoque estoque(Ingrediente i) {
        return new Estoque(i.id(), i.nome(), i.tipo(), i.quantidade(), i.tipo().unidade(),
            i.compartimento().id(), i.compartimento().codigo(), i.criadoEm(), i.atualizadoEm());
    }

    private static LocalDate dia(Instant instante) {
        return instante.atZone(FUSO).toLocalDate();
    }

    private static void validarQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0 || quantidade.scale() > 3
                || (long) quantidade.precision() - quantidade.scale() > 9) {
            throw ErroDeNegocio.invalido("Informe uma quantidade positiva com até nove dígitos inteiros e três casas decimais.");
        }
    }

    private static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank() || valor.length() > 120) {
            throw ErroDeNegocio.invalido(campo + " deve conter de 1 a 120 caracteres.");
        }
        return valor.strip();
    }

    private static Tipo categoria(Tipo tipo) {
        if (tipo == null) {
            throw ErroDeNegocio.invalido("Informe a categoria do ingrediente.");
        }
        return tipo;
    }
}
