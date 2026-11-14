package com.vidacruz.triage.infraestructura.entrada.rest;

import com.vidacruz.triage.dominio.excepcion.ExcepcionPacienteNoEncontrado;
import com.vidacruz.triage.dominio.excepcion.ExcepcionTransicionInvalida;
import com.vidacruz.triage.dominio.excepcion.ExcepcionValorInvalido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;

@RestControllerAdvice
public class ManejadorGlobalErrores {
    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    @ExceptionHandler(ExcepcionValorInvalido.class)
    public ResponseEntity<RespuestaError> valorInvalido(ExcepcionValorInvalido ex, ServerWebExchange exchange) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.campo(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<RespuestaError> validacionBean(WebExchangeBindException ex, ServerWebExchange exchange) {
        FieldError error = ex.getFieldError();
        String campo = error == null ? null : error.getField();
        String mensaje = error == null ? "Solicitud inválida"
                : "El campo " + campo + " " + error.getDefaultMessage();
        return responder(HttpStatus.BAD_REQUEST, mensaje, campo, exchange);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<RespuestaError> entradaMalFormada(ServerWebInputException ex, ServerWebExchange exchange) {
        String causa = ex.getMostSpecificCause().getMessage();
        String mensaje = "Solicitud mal formada: " + (causa == null ? ex.getReason() : resumir(causa));
        return responder(HttpStatus.BAD_REQUEST, mensaje, null, exchange);
    }

    @ExceptionHandler(ExcepcionTransicionInvalida.class)
    public ResponseEntity<RespuestaError> transicionInvalida(ExcepcionTransicionInvalida ex, ServerWebExchange exchange) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), "estado", exchange);
    }

    @ExceptionHandler(ExcepcionPacienteNoEncontrado.class)
    public ResponseEntity<RespuestaError> noEncontrado(ExcepcionPacienteNoEncontrado ex, ServerWebExchange exchange) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), null, exchange);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespuestaError> conEstado(ResponseStatusException ex, ServerWebExchange exchange) {
        HttpStatus estado = HttpStatus.valueOf(ex.getStatusCode().value());
        return responder(estado, ex.getReason() == null ? estado.getReasonPhrase() : ex.getReason(), null, exchange);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception ex, ServerWebExchange exchange) {
        log.error("Error no controlado en {}", exchange.getRequest().getPath(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno. Revise los logs con el traceId de la petición.",
                null, exchange);
    }

    private static ResponseEntity<RespuestaError> responder(HttpStatus estado, String mensaje, String campo,
                                                            ServerWebExchange exchange) {
        RespuestaError cuerpo = new RespuestaError(Instant.now(), estado.value(), estado.getReasonPhrase(),
                mensaje, campo, exchange.getRequest().getPath().value());
        return ResponseEntity.status(estado).body(cuerpo);
    }

    private static String resumir(String causa) {
        int corte = causa.indexOf('\n');
        String primera = corte > 0 ? causa.substring(0, corte) : causa;
        return primera.length() > 200 ? primera.substring(0, 200) + "…" : primera;
    }
}
