package com.paboomi.backend.util.ring;

import com.paboomi.backend.util.RandomAccessFileUtil;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manejador de índices secundarios (clave -> lista de posiciones)
 * [clave(longitud fija)] + [cantidad(int)] + [pos1(long)] + [pos2(long)] + ...
 */
public class SecondaryIndexHandler {

    private final String rutaArchivo;
    private final int longitudClave;
    private final String rutaArchivoDatos;

    public SecondaryIndexHandler(String rutaArchivo,
                                 int longitudClave,
                                 String rutaArchivoDatos) {
        this.rutaArchivo = rutaArchivo;
        this.longitudClave = longitudClave;
        this.rutaArchivoDatos = rutaArchivoDatos;
        inicializarArchivo();
    }

    private void inicializarArchivo() {
        try {
            new RandomAccessFile(rutaArchivo, "rw").close();
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar índice secundario: " + rutaArchivo, e);
        }
    }

     //* Agrega una posición a la lista de una clave
    public void agregar(String clave, long posicion) throws Exception {

        //* Obtener lista actual
        List<Long> posiciones = buscar(clave);
        if (posiciones == null) {
            posiciones = new ArrayList<>();
        }

        if (posiciones.contains(posicion)) {
            return;
        }

        posiciones.add(posicion);
        guardar(clave, posiciones);
    }

     //* Busca las posiciones asociadas a una clave
     public List<Long> buscar(String clave) throws Exception {
         List<Long> todas = buscarTodas(clave);
         if (todas == null || todas.isEmpty()) {
             return new ArrayList<>();
         }

         List<Long> validos = new ArrayList<>();
         try (RandomAccessFile rafDatos = new RandomAccessFile(rutaArchivoDatos, "r")) {
             for (Long posicion : todas) {
                 rafDatos.seek(posicion);
                 boolean ocupado = rafDatos.readBoolean();
                 if (ocupado) {
                     validos.add(posicion);
                 }
             }
         }
         return validos;
     }


     //* Busca TODAS las posiciones
    private List<Long> buscarTodas(String clave) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                String claveLeida = RandomAccessFileUtil.leerCadenaFija(raf, longitudClave);
                int cantidad = raf.readInt();
                List<Long> posiciones = new ArrayList<>();
                for (int i = 0; i < cantidad; i++) {
                    posiciones.add(raf.readLong());
                }
                if (claveLeida.equals(clave)) {
                    return posiciones;
                }
            }
        }
        return null;
    }

    //* Elimina una posición específica de la lista de una clave
    public void eliminar(String clave, long posicion) throws Exception {

        List<Long> posiciones = buscarTodas(clave);
        if (posiciones == null || posiciones.isEmpty()) return;

        boolean removido = posiciones.remove(posicion);

        if (!removido) return;

        guardar(clave, posiciones);

    }

    /**
     * Elimina completamente una clave del índice
     */
    private void eliminarClaveCompleta(String clave) throws Exception {
        String tempFile = rutaArchivo + ".temp";
        try (RandomAccessFile rafOriginal = new RandomAccessFile(rutaArchivo, "r");
             RandomAccessFile rafTemp = new RandomAccessFile(tempFile, "rw")) {

            while (rafOriginal.getFilePointer() < rafOriginal.length()) {
                long posInicio = rafOriginal.getFilePointer();
                String claveLeida = RandomAccessFileUtil.leerCadenaFija(rafOriginal, longitudClave);
                int cantidad = rafOriginal.readInt();

                //* Saltar las posiciones
                for (int i = 0; i < cantidad; i++) {
                    rafOriginal.readLong();
                }

                if (!claveLeida.equals(clave)) {
                    //* Copiar entrada al archivo temporal
                    long posFin = rafOriginal.getFilePointer();
                    rafOriginal.seek(posInicio);
                    byte[] entrada = new byte[(int) (posFin - posInicio)];
                    rafOriginal.readFully(entrada);
                    rafTemp.write(entrada);
                }
            }
        }

        Files.delete(Paths.get(rutaArchivo));
        Files.move(Paths.get(tempFile), Paths.get(rutaArchivo));
    }


     //* Guarda una lista de posiciones para una clave
    private void guardar(String clave, List<Long> posiciones) throws Exception {

        //* Primero eliminar entrada existente
        eliminarClaveCompleta(clave);

        //* Luego agregar la nueva
        try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "rw")) {
            raf.seek(raf.length());
            RandomAccessFileUtil.escribirCadenaFija(raf, clave, longitudClave);
            raf.writeInt(posiciones.size());
            for (Long pos : posiciones) {
                raf.writeLong(pos);
            }
        }
    }

     //* Carga todo el índice en memoria
    public Map<String, List<Long>> cargarTodos() throws Exception {
        Map<String, List<Long>> mapa = new HashMap<>();
        try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                String clave = RandomAccessFileUtil.leerCadenaFija(raf, longitudClave);
                int cantidad = raf.readInt();
                List<Long> posiciones = new ArrayList<>();
                for (int i = 0; i < cantidad; i++) {
                    posiciones.add(raf.readLong());
                }
                mapa.put(clave, posiciones);
            }
        }
        return mapa;
    }
}