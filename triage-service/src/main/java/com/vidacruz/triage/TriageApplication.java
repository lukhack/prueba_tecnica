package com.vidacruz.triage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Servicio de Triage: registro, clasificación, SLA/escalamiento, auditoría y reportes. */
@SpringBootApplication
public class TriageApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriageApplication.class, args);
    }
}
