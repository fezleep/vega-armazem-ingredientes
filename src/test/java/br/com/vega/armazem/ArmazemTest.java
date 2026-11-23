package br.com.vega.armazem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:armazem;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc
class ArmazemTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean Clock clock;

    @BeforeEach
    void limpar() {
        jdbc.update("DELETE FROM movimentacao");
        jdbc.update("DELETE FROM ingrediente");
        jdbc.update("UPDATE compartimento SET ultimo_tipo=NULL, ultima_data=NULL");
        quando("2026-01-02T02:59:59Z");
    }

    @Test
    void inicializaCincoCompartimentos() throws Exception {
        mvc.perform(get("/compartimentos")).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].codigo", contains("C1", "C2", "C3", "C4", "C5")));
    }

    @ParameterizedTest
    @CsvSource({"SECO,600", "LIQUIDO,500", "REFRIGERADO,400"})
    void aceitaLimiteExato(String tipo, String quantidade) throws Exception {
        criar("Primeiro", tipo, quantidade, 1).andExpect(status().isCreated());
        criar("Segundo", tipo, "0.001", 1).andExpect(status().isBadRequest());
        contarHistorico(1);
    }

    @Test
    void cadastroAliasCapacidadeConjuntaEDuplicidade() throws Exception {
        criar(" Farinha ", "SECOS", "400", 1).andExpect(status().isCreated())
            .andExpect(header().exists("Location")).andExpect(jsonPath("$.tipo").value("SECO"));
        criar("Aveia", "SECO", "100", 1).andExpect(status().isCreated());
        criar("farinha", "SECO", "1", 1).andExpect(status().isConflict());
        criar("Água", "LIQUIDO", "1", 1).andExpect(status().isBadRequest());
        mvc.perform(get("/compartimentos/disponiveis?tipo=SECO&quantidade=100.001"))
            .andExpect(jsonPath("$[*].id", contains(2, 3, 4, 5)));
        mvc.perform(get("/ingredientes/volume")).andExpect(jsonPath("$[0].totalQuantity").value(500))
            .andExpect(jsonPath("$[1].totalQuantity").value(0))
            .andExpect(jsonPath("$[2].totalQuantity").value(0));
        contarHistorico(2);
    }

    @ParameterizedTest
    @CsvSource({"Farinha,SECO,0,1","Farinha,SECO,-1,1","Farinha,SECO,1.0001,1",
        "Farinha,QUALQUER,1,1","' ',SECO,1,1","Farinha,SECO,1,0"})
    void rejeitaCadastroInvalido(String nome, String tipo, String quantidade, int compartimento) throws Exception {
        criar(nome, tipo, quantidade, compartimento).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isNotEmpty());
        contarHistorico(0);
    }

    @Test
    void responsavelObrigatorioEIdInexistente() throws Exception {
        enviar("/ingredientes", Map.of("nome","A","tipo","SECO","quantidade",1,"compartimentoId",1,
            "responsavel"," ")).andExpect(status().isBadRequest());
        criar("A", "SECO", "1", 6).andExpect(status().isNotFound());
        mvc.perform(get("/ingredientes/999999")).andExpect(status().isNotFound());
    }

    @Test
    void rejeitaCoercaoDeCategoriaEIdFracionario() throws Exception {
        enviar("/ingredientes", Map.of("nome","A","tipo",0,"quantidade",1,"compartimentoId",1,
            "responsavel","Equipe")).andExpect(status().isBadRequest());
        enviar("/ingredientes", Map.of("nome","A","tipo","SECO","quantidade",1,"compartimentoId",1.9,
            "responsavel","Equipe")).andExpect(status().isBadRequest());
        mvc.perform(get("/ingredientes")).andExpect(jsonPath("$.length()").value(0));
        contarHistorico(0);
    }

    @Test
    void viradaDoDiaCivilRevalidaIngredienteZerado() throws Exception {
        var id = id(criar("Farinha", "SECO", "10", 1));
        mover(id, "entradas", "5").andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value(15));
        mover(id, "retiradas", "16").andExpect(status().isBadRequest());
        mover(id, "retiradas", "5").andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value(10));
        mover(id, "retiradas", "10").andExpect(status().isOk());
        mvc.perform(get("/ingredientes/" + id)).andExpect(jsonPath("$.quantidade").value(0));
        mvc.perform(get("/compartimentos/disponiveis-para-venda?tipo=SECO")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/compartimentos/disponiveis?tipo=LIQUIDO&quantidade=1"))
            .andExpect(jsonPath("$[*].id", contains(2, 3, 4, 5)));
        criar("Água", "LIQUIDO", "1", 1).andExpect(status().isBadRequest());
        mover(id, "entradas", "1").andExpect(status().isOk());
        mover(id, "retiradas", "1").andExpect(status().isOk());
        quando("2026-01-02T03:00:00Z");
        criar("Água", "LIQUIDO", "1", 1).andExpect(status().isCreated());
        mover(id, "entradas", "1").andExpect(status().isBadRequest());
        contarHistorico(7);
    }

    @Test
    void retiradaEmOutroDiaAtualizaBloqueio() throws Exception {
        var id = id(criar("Farinha", "SECO", "10", 1));
        quando("2026-01-02T03:00:00Z");
        mover(id, "retiradas", "10").andExpect(status().isOk());
        criar("Água", "LIQUIDO", "1", 1).andExpect(status().isBadRequest());
    }

    @Test
    void ordenaHistoricoComDesempateEConsultaVenda() throws Exception {
        criar("A", "SECO", "1", 2).andExpect(status().isCreated());
        criar("B", "SECO", "1", 1).andExpect(status().isCreated());
        criar("C", "SECO", "1", 2).andExpect(status().isCreated());
        quando("2026-01-02T03:00:00Z");
        criar("D", "LIQUIDO", "1", 1).andExpect(status().isBadRequest());
        criar("D", "LIQUIDO", "1", 3).andExpect(status().isCreated());
        mvc.perform(get("/historico")).andExpect(jsonPath("$[*].ingredienteNome", contains("D", "C", "A", "B")));
        mvc.perform(get("/historico?sortBy=compartimento&order=asc"))
            .andExpect(jsonPath("$[*].ingredienteNome", contains("B", "A", "C", "D")));
        mvc.perform(get("/historico?sortBy=id")).andExpect(status().isBadRequest());
        mvc.perform(get("/historico?order=mais")).andExpect(status().isBadRequest());
        mvc.perform(get("/historico?order=")).andExpect(status().isBadRequest());
        mvc.perform(get("/compartimentos/disponiveis-para-venda?tipo=SECO"))
            .andExpect(jsonPath("$[0].quantidadeTotal").value(1))
            .andExpect(jsonPath("$[1].quantidadeTotal").value(2))
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void entradasConcorrentesRespeitamCapacidade() throws Exception {
        var a = id(criar("A", "SECO", "250", 1));
        var b = id(criar("B", "SECO", "250", 1));
        competir(() -> codigo(mover(a,"entradas","100")), () -> codigo(mover(b,"entradas","100")), 200);
        mvc.perform(get("/ingredientes/volume")).andExpect(jsonPath("$[0].totalQuantity").value(600));
        contarHistorico(3);
    }

    @Test
    void retiradasConcorrentesRespeitamSaldo() throws Exception {
        var id = id(criar("A", "SECO", "10", 1));
        competir(() -> codigo(mover(id,"retiradas","8")), () -> codigo(mover(id,"retiradas","8")), 200);
        mvc.perform(get("/ingredientes/" + id)).andExpect(jsonPath("$.quantidade").value(2));
        contarHistorico(2);
    }

    @Test
    void categoriasConcorrentesNaoMisturam() throws Exception {
        competir(() -> codigo(criar("A","SECO","10",1)), () -> codigo(criar("B","LIQUIDO","10",1)), 201);
        mvc.perform(get("/ingredientes")).andExpect(jsonPath("$.length()").value(1));
        contarHistorico(1);
    }

    @Test
    void falhaAoGravarHistoricoReverteSaldo() throws Exception {
        var id = id(criar("A", "SECO", "10", 1));
        jdbc.execute("ALTER TABLE movimentacao ADD CONSTRAINT teste_rollback CHECK (responsavel <> 'falha')");
        try {
            enviar("/ingredientes", Map.of("nome","B","tipo","LIQUIDO","quantidade",5,
                "compartimentoId",2,"responsavel","falha")).andExpect(status().isConflict());
            enviar("/ingredientes/" + id + "/entradas", Map.of("quantidade",5,"responsavel","falha"))
                .andExpect(status().isConflict());
            mvc.perform(get("/ingredientes/" + id)).andExpect(jsonPath("$.quantidade").value(10));
            mvc.perform(get("/ingredientes")).andExpect(jsonPath("$.length()").value(1));
            mvc.perform(get("/compartimentos")).andExpect(jsonPath("$[1].disponivel").value(true))
                .andExpect(jsonPath("$[1].ultimoTipoUtilizado").isEmpty());
            contarHistorico(1);
        } finally {
            jdbc.execute("ALTER TABLE movimentacao DROP CONSTRAINT teste_rollback");
        }
    }

    private void competir(Callable<Integer> primeira, Callable<Integer> segunda, int sucesso) throws Exception {
        var barreira = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { barreira.await(5, TimeUnit.SECONDS); return primeira.call(); });
            var b = pool.submit(() -> { barreira.await(5, TimeUnit.SECONDS); return segunda.call(); });
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(sucesso, 400);
        }
    }

    private void quando(String instante) { when(clock.instant()).thenReturn(Instant.parse(instante)); }
    private void contarHistorico(int total) throws Exception {
        mvc.perform(get("/historico")).andExpect(jsonPath("$.length()").value(total));
    }
    private long id(ResultActions resposta) throws Exception {
        return json.readTree(resposta.andExpect(status().isCreated()).andReturn().getResponse()
            .getContentAsByteArray()).get("id").asLong();
    }
    private int codigo(ResultActions resposta) { return resposta.andReturn().getResponse().getStatus(); }
    private ResultActions criar(String nome, String tipo, String quantidade, int compartimento) throws Exception {
        return enviar("/ingredientes", Map.of("nome",nome,"tipo",tipo,"quantidade",new BigDecimal(quantidade),
            "compartimentoId",compartimento,"responsavel","Equipe"));
    }
    private ResultActions mover(long id, String acao, String quantidade) throws Exception {
        return enviar("/ingredientes/" + id + "/" + acao,
            Map.of("quantidade",new BigDecimal(quantidade),"responsavel","Equipe"));
    }
    private ResultActions enviar(String rota, Map<String,?> corpo) throws Exception {
        return mvc.perform(post(rota).contentType("application/json").content(json.writeValueAsBytes(corpo)));
    }
}
