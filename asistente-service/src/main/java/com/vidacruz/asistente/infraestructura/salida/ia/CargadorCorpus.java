package com.vidacruz.asistente.infraestructura.salida.ia;

import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee el corpus oficial desde {@code classpath:protocolos/*.md} y lo parte en fragmentos: uno por sección.
 *
 * <p>Formato de cada archivo (Markdown sencillo, fácil de extender con protocolos propios):</p>
 * <pre>
 * # PROTO-001 — Crisis Hipertensiva, manejo inicial
 * ## Definición
 * texto...
 * ## Medidas iniciales
 * texto...
 * </pre>
 */
public class CargadorCorpus {

    static final String UBICACION = "classpath:protocolos/*.md";

    private static final Pattern ENCABEZADO = Pattern.compile("^#\\s+(PROTO-\\d+)\\s*[—–-]\\s*(.+)$");
    private static final Pattern SECCION = Pattern.compile("^##\\s+(.+)$");

    public List<FragmentoProtocolo> cargar() {
        try {
            Resource[] archivos = new PathMatchingResourcePatternResolver().getResources(UBICACION);
            List<FragmentoProtocolo> fragmentos = new ArrayList<>();
            Arrays.stream(archivos)
                    .sorted((a, b) -> String.valueOf(a.getFilename()).compareTo(String.valueOf(b.getFilename())))
                    .forEach(archivo -> fragmentos.addAll(parsear(leer(archivo), String.valueOf(archivo.getFilename()))));
            if (fragmentos.isEmpty()) {
                throw new IllegalStateException("El corpus de protocolos está vacío (" + UBICACION + ")");
            }
            return fragmentos;
        } catch (IOException e) {
            throw new UncheckedIOException("No fue posible leer el corpus de protocolos", e);
        }
    }

    static List<FragmentoProtocolo> parsear(String markdown, String origen) {
        String codigo = null;
        String titulo = null;
        String seccion = null;
        StringBuilder contenido = new StringBuilder();
        List<FragmentoProtocolo> fragmentos = new ArrayList<>();

        for (String linea : markdown.split("\\R")) {
            Matcher cabecera = ENCABEZADO.matcher(linea.trim());
            if (cabecera.matches()) {
                codigo = cabecera.group(1).trim();
                titulo = cabecera.group(2).trim();
                continue;
            }
            Matcher nuevaSeccion = SECCION.matcher(linea.trim());
            if (nuevaSeccion.matches()) {
                agregar(fragmentos, codigo, titulo, seccion, contenido, origen);
                seccion = nuevaSeccion.group(1).trim();
                contenido.setLength(0);
                continue;
            }
            if (seccion != null) {
                contenido.append(linea.trim()).append('\n');
            }
        }
        agregar(fragmentos, codigo, titulo, seccion, contenido, origen);
        return fragmentos;
    }

    private static void agregar(List<FragmentoProtocolo> destino, String codigo, String titulo, String seccion,
                                StringBuilder contenido, String origen) {
        if (seccion == null) {
            return;
        }
        if (codigo == null) {
            throw new IllegalStateException("El archivo " + origen + " no empieza con '# PROTO-XXX — Título'");
        }
        String texto = contenido.toString().trim();
        if (!texto.isEmpty()) {
            destino.add(new FragmentoProtocolo(codigo, titulo, seccion, texto, 0.0));
        }
    }

    private static String leer(Resource recurso) {
        try {
            return recurso.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No fue posible leer " + recurso.getFilename(), e);
        }
    }
}
