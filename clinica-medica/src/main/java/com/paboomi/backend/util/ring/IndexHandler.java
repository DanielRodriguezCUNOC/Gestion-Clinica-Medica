package com.paboomi.backend.util.ring;

import com.paboomi.backend.util.files.RandomAccessFileUtil;
import com.paboomi.backend.util.exceptions.FileException;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador de índices simples [clave(longitud fija)] + [posición(long)]
 */
public class IndexHandler {

    private final String rutaArchivo;
    private final int longitudClave;
    private final int tamanioEntrada;

    public IndexHandler(String rutaArchivo, int longitudClave) {
        this.rutaArchivo = rutaArchivo;
        this.longitudClave = longitudClave;
        //! 2 bytes por carácter + 8 bytes para long
        this.tamanioEntrada = (longitudClave * 2) + 8;
        inicializarArchivo();
    }

    private void inicializarArchivo() {
        try {
            new RandomAccessFile(rutaArchivo, "rw").close();
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar índice: " + rutaArchivo, e);
        }
    }

    //* Agrega una entrada al índice
    public void agregar(String clave, long posicion) throws Exception {
        // Verificar que no exista
        if (buscar(clave) != null) {
            throw new FileException("La clave ya existe en el índice: " + clave);
        }

        try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "rw")) {
            raf.seek(raf.length());
            RandomAccessFileUtil.escribirCadenaFija(raf, clave, longitudClave);
            raf.writeLong(posicion);
        }
    }
    //* Busca una clave en el índice
    public Long buscar(String clave) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                String claveLeida = RandomAccessFileUtil.leerCadenaFija(raf, longitudClave);
                long posicion = raf.readLong();
                if (claveLeida.equals(clave)) {
                    return posicion;
                }
            }
        }
        return null;
    }
    //* Elimina una entrada del índice
    public void eliminar(String clave) throws Exception {
        String tempFile = rutaArchivo + ".temp";
        try (RandomAccessFile rafOriginal = new RandomAccessFile(rutaArchivo, "r");
             RandomAccessFile rafTemp = new RandomAccessFile(tempFile, "rw")) {

            while (rafOriginal.getFilePointer() < rafOriginal.length()) {
                long posInicio = rafOriginal.getFilePointer();
                String claveLeida = RandomAccessFileUtil.leerCadenaFija(rafOriginal, longitudClave);
                long posicion = rafOriginal.readLong();

                if (!claveLeida.equals(clave)) {
                    // Copiar entrada al archivo temporal
                    rafOriginal.seek(posInicio);
                    byte[] entrada = new byte[tamanioEntrada];
                    rafOriginal.readFully(entrada);
                    rafTemp.write(entrada);
                }
            }
        }

        //* Reemplazar archivo original

        Files.delete(Paths.get(rutaArchivo));
        Files.move(java.nio.file.Paths.get(tempFile), Paths.get(rutaArchivo));
    }

    //* Carga todo el índice en memoria
    public Map<String, Long> cargarTodos() throws Exception {
        Map<String, Long> mapa = new HashMap<>();
        try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                String clave = RandomAccessFileUtil.leerCadenaFija(raf, longitudClave);
                long posicion = raf.readLong();
                mapa.put(clave, posicion);
            }
        }
        return mapa;
    }

    //* Verifica si una clave existe en el índice
    public boolean existe(String clave) throws Exception {
        return buscar(clave) != null;
    }

    //* Obtiene la cantidad de entradas en el índice
    public long getCantidadEntradas() throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "r")) {
            return raf.length() / tamanioEntrada;
        }
    }
}