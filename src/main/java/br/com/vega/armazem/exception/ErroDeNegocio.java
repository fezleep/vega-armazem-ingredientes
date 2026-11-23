package br.com.vega.armazem.exception;

import org.springframework.http.HttpStatus;

public class ErroDeNegocio extends RuntimeException {
    private final HttpStatus status;

    private ErroDeNegocio(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus status() { return status; }

    public static ErroDeNegocio invalido(String mensagem) {
        return new ErroDeNegocio(HttpStatus.BAD_REQUEST, mensagem);
    }

    public static ErroDeNegocio inexistente(String mensagem) {
        return new ErroDeNegocio(HttpStatus.NOT_FOUND, mensagem);
    }

    public static ErroDeNegocio conflito(String mensagem) {
        return new ErroDeNegocio(HttpStatus.CONFLICT, mensagem);
    }
}
