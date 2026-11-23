package br.com.vega.armazem.controller;

import br.com.vega.armazem.domain.Operacao;
import br.com.vega.armazem.domain.Tipo;
import br.com.vega.armazem.dto.Contratos.*;
import br.com.vega.armazem.service.ArmazemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
public class ArmazemController {
    private final ArmazemService armazem;

    public ArmazemController(ArmazemService armazem) {
        this.armazem = armazem;
    }

    @PostMapping("/ingredientes")
    @Operation(summary = "Cadastrar ingrediente e registrar a entrada inicial")
    @ApiResponse(responseCode = "201", description = "Ingrediente criado")
    public ResponseEntity<Estoque> cadastrar(@Valid @RequestBody Cadastro cadastro) {
        var estoque = armazem.cadastrar(cadastro);
        return ResponseEntity.created(URI.create("/ingredientes/" + estoque.id())).body(estoque);
    }

    @GetMapping("/ingredientes")
    @Operation(summary = "Listar ingredientes, inclusive com saldo zero")
    public List<Estoque> ingredientes() {
        return armazem.listarIngredientes();
    }

    @GetMapping("/ingredientes/{id}")
    @Operation(summary = "Consultar ingrediente por ID")
    public Estoque ingrediente(@PathVariable @Positive long id) {
        return armazem.consultarIngrediente(id);
    }

    @GetMapping("/ingredientes/volume")
    @Operation(summary = "Somar o saldo atual por categoria")
    public List<Volume> volumes() {
        return armazem.volumes();
    }

    @PostMapping("/ingredientes/{id}/entradas")
    @Operation(summary = "Repor estoque de um ingrediente")
    public Saldo entrada(@PathVariable @Positive long id, @Valid @RequestBody Ajuste ajuste) {
        return armazem.movimentar(id, ajuste, Operacao.ENTRADA);
    }

    @PostMapping("/ingredientes/{id}/retiradas")
    @Operation(summary = "Retirar estoque de um ingrediente")
    public Saldo retirada(@PathVariable @Positive long id, @Valid @RequestBody Ajuste ajuste) {
        return armazem.movimentar(id, ajuste, Operacao.SAIDA);
    }

    @GetMapping("/compartimentos")
    @Operation(summary = "Consultar o estado dos cinco compartimentos")
    public List<Estado> compartimentos() {
        return armazem.estados();
    }

    @GetMapping("/compartimentos/disponiveis")
    @Operation(summary = "Encontrar compartimentos que comportam toda a entrada solicitada")
    public List<Estado> disponiveis(@RequestParam Tipo tipo, @RequestParam BigDecimal quantidade) {
        return armazem.disponiveis(tipo, quantidade);
    }

    @GetMapping("/compartimentos/disponiveis-para-venda")
    @Operation(summary = "Encontrar compartimentos com saldo positivo da categoria")
    public List<Estado> paraVenda(@RequestParam Tipo tipo) {
        return armazem.disponiveisParaVenda(tipo);
    }

    @GetMapping("/historico")
    @Operation(summary = "Consultar histórico", description = "sortBy aceita date ou compartimento. order aceita asc ou desc.")
    public List<Evento> historico(@RequestParam(required = false) String sortBy,
                                 @RequestParam(required = false) String order) {
        return armazem.historico(sortBy == null ? "date" : sortBy, order == null ? "desc" : order);
    }
}
