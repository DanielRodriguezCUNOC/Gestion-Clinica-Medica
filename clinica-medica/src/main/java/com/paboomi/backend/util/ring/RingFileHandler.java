package com.paboomi.backend.util.ring;

import com.paboomi.backend.util.exceptions.FileException;
import lombok.Getter;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Manejador de archivos de anillo con bitmap de espacios libres.
 */
@Getter
public class RingFileHandler {

    private final String rutaArchivo;
    private final int tamanioRegistro;  //! Incluye bitmap
    private final List<Integer> espaciosLibres;
    private boolean cacheActualizada;

    public RingFileHandler(String rutaArchivo, int tamanioRegistroConBitmap) {
        this.rutaArchivo = rutaArchivo;
        this.tamanioRegistro = tamanioRegistroConBitmap;
        this.espaciosLibres = new ArrayList<>();
        this.cacheActualizada = false;
        inicializarArchivo();
    }


    private void inicializarArchivo() {
        try {
            new RandomAccessFile(rutaArchivo, "rw").close();
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar archivo: " + rutaArchivo, e);
        }
    }

    // * Recarga la lista de espacios libres escaneando el bitmap
    public void recargarEspaciosLibres() throws Exception {
        espaciosLibres.clear();
        try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "r")) {
            long cantidadRegistros = raf.length() / tamanioRegistro;
            for (int i = 0; i < cantidadRegistros; i++) {
                raf.seek((long) i * tamanioRegistro);
                boolean ocupado = raf.readBoolean();
                if (!ocupado) {
                    espaciosLibres.add(i);
                }
            }
        }
        cacheActualizada = true;
    }

     //* Obtiene un espacio libre
    public int obtenerEspacioLibre() throws Exception {
        if (!cacheActualizada) {
            recargarEspaciosLibres();
        }

        if (!espaciosLibres.isEmpty()) {
            return espaciosLibres.remove(0);
        } else {

            //* Crear nuevo registro al final
            try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "r")) {
                return (int) (raf.length() / tamanioRegistro);
            }
        }
    }

    //* Marca un registro como ocupado o libre
    public void marcarEstado(RandomAccessFile raf, boolean ocupado) throws Exception {
        raf.writeBoolean(ocupado);
        if (!ocupado) {
            int posRegistro = (int) (raf.getFilePointer() / tamanioRegistro) - 1;
            espaciosLibres.add(posRegistro);
        }
    }

    //* Verifica si un registro está ocupado
    public boolean estaOcupado(RandomAccessFile raf) throws Exception {
        return raf.readBoolean();
    }


    //* Calcula la posición física a partir del número de registro
    public long getPosicionFisica(int numeroRegistro) {
        return (long) numeroRegistro * tamanioRegistro;
    }

    //* Obtiene la cantidad total de registros en el archivo
    public long getCantidadRegistros() throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "r")) {
            return raf.length() / tamanioRegistro;
        }
    }

     //* Libera un espacio específico
    public void liberarEspacio(int numeroRegistro) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(rutaArchivo, "rw")) {
            raf.seek(getPosicionFisica(numeroRegistro));
            marcarEstado(raf, false);
        }
        if (!espaciosLibres.contains(numeroRegistro)) {
            espaciosLibres.add(numeroRegistro);
        }
    }
}