package com.paboomi.backend.util.ring;

import java.io.RandomAccessFile;

/**
 * Interfaz genérica para serializar/deserializar entidades en archivos de anillo.
 */
public interface Serializador<T> {

    //* Escribir una entidad en el archivo en la posicion actual del archivo
    void escribir(RandomAccessFile raf, T entidad) throws Exception;

    //* Lee una entidad desde la posicion actual del archivo
    T leer(RandomAccessFile raf) throws Exception;

    //* obtiene el tamaño del registro en bytes sin el bitmap
    int getTamanioRegistro();
}