package com.vidacruz.asistente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Servicio de Asistente Clínico: consulta de protocolos con RAG y estado de urgencias en tiempo real. */
@SpringBootApplication
public class AsistenteApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsistenteApplication.class, args);
    }
}
