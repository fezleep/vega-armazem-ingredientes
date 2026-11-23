package br.com.vega.armazem.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class TratamentoDeErros {
    private static final Logger LOG = LoggerFactory.getLogger(TratamentoDeErros.class);
    private final Clock clock;

    public TratamentoDeErros(Clock clock) {
        this.clock = clock;
    }

    record Erro(Instant timestamp, int status, String error, String message, String path, Map<String, String> fields) {}

    @ExceptionHandler(ErroDeNegocio.class)
    ResponseEntity<Erro> negocio(ErroDeNegocio erro, HttpServletRequest req) {
        return resposta(erro.status(), erro.getMessage(), req, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Erro> campos(MethodArgumentNotValidException erro, HttpServletRequest req) {
        Map<String, String> campos = new LinkedHashMap<>();
        erro.getBindingResult().getFieldErrors().forEach(c -> campos.putIfAbsent(c.getField(), c.getDefaultMessage()));
        return resposta(HttpStatus.BAD_REQUEST, "Verifique os campos informados.", req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Erro> corpo(HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo JSON inválido. Categorias aceitas: SECO, SECOS, LIQUIDO e REFRIGERADO.", req, Map.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
        HandlerMethodValidationException.class})
    ResponseEntity<Erro> parametros(HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Confira os parâmetros e IDs informados.", req, Map.of());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    ResponseEntity<Erro> conflito(HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, "A operação encontrou um conflito de integridade ou concorrência.", req, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Erro> ausente(HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso inexistente.", req, Map.of());
    }

    @ExceptionHandler(ErrorResponseException.class)
    ResponseEntity<Erro> protocolo(ErrorResponseException erro, HttpServletRequest req) {
        return resposta(HttpStatus.valueOf(erro.getStatusCode().value()), "A requisição não pôde ser processada.", req, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Erro> metodo(HttpRequestMethodNotSupportedException erro, HttpServletRequest req) {
        var status = HttpStatus.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(status).headers(erro.getHeaders())
            .body(new Erro(clock.instant(), status.value(), status.getReasonPhrase(),
                "Método HTTP indisponível para este recurso.", req.getRequestURI(), Map.of()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Erro> formato(HttpServletRequest req) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie o corpo da requisição em JSON.", req, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Erro> inesperado(Exception erro, HttpServletRequest req) {
        LOG.error("Falha inesperada ao processar requisição", erro);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Falha inesperada ao processar a requisição.", req, Map.of());
    }

    private ResponseEntity<Erro> resposta(HttpStatus status, String mensagem, HttpServletRequest req,
                                          Map<String, String> campos) {
        return ResponseEntity.status(status).body(new Erro(clock.instant(), status.value(),
            status.getReasonPhrase(), mensagem, req.getRequestURI(), campos));
    }
}
