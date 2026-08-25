package com.paboomi.backend.util.files;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomAccessFileUtil {

    public static void escribirCadenaFija(RandomAccessFile raf, String texto, int longitudFija) throws IOException {
        StringBuilder sb = new StringBuilder(texto != null ? texto : "");
        sb.setLength(longitudFija);
        raf.writeChars(sb.toString());
    }

    // También podrías agregar el método para leer
    public static String leerCadenaFija(RandomAccessFile raf, int longitudFija) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < longitudFija; i++) {
            sb.append(raf.readChar());
        }
        return sb.toString().trim(); // trim() elimina espacios extra
    }
}