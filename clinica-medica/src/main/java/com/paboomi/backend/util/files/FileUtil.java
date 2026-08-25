package com.paboomi.backend.util.files;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
    /**
     * Crea los directorios necesarios para un archivo si no existen
     */
    public static void crearDirectoriosSiNoExisten(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Crea los directorios necesarios para una lista de archivos
     */
    public static void crearDirectoriosParaArchivos(String... filePaths) throws Exception {
        for (String filePath : filePaths) {
            crearDirectoriosSiNoExisten(filePath);
        }
    }

}
