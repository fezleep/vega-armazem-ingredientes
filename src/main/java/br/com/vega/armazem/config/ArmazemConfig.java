package br.com.vega.armazem.config;

import java.time.Clock;
import java.time.ZoneId;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Armazém de Ingredientes", version = "1.0",
    description = "Estoque e movimentações. Instantes em ISO-8601 UTC e calendário em America/Sao_Paulo."))
public class ArmazemConfig {
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
