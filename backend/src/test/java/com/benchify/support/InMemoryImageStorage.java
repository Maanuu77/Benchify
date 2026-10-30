package com.benchify.support;

import com.benchify.banco.application.port.out.AlmacenImagenesPort;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sustituye a Cloud Storage en los tests de integración. */
public class InMemoryImageStorage implements AlmacenImagenesPort {

    public static final String BASE_URL = "https://storage.test/";

    private final Map<String, byte[]> ficheros = new ConcurrentHashMap<>();

    @Override
    public String guardar(String ruta, byte[] contenido, String contentType) {
        ficheros.put(ruta, contenido);
        return BASE_URL + ruta;
    }

    @Override
    public void borrar(String ruta) {
        ficheros.remove(ruta);
    }

    public boolean contiene(String ruta) {
        return ficheros.containsKey(ruta);
    }

    /** Rutas guardadas bajo una carpeta, p. ej. la de un banco. */
    public List<String> rutas(String prefijo) {
        return ficheros.keySet().stream().filter(ruta -> ruta.startsWith(prefijo)).sorted().toList();
    }
}
