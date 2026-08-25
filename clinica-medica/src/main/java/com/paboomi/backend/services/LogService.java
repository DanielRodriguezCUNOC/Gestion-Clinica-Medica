package com.paboomi.backend.services;

import com.paboomi.backend.models.LogEntry;
import com.paboomi.backend.util.files.FileUtil;
import com.paboomi.backend.util.files.RandomAccessFileUtil;
import com.paboomi.backend.util.exceptions.ServiceException;
import lombok.Getter;
import lombok.Setter;

import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class LogService {

    private static final String RUTA_LOG = "data/logs/logs.dat";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    //* Tamaño máximo de una línea de log
    private static final int LONGITUD_MAXIMA_LOG = 500;

    private String usuarioActual;

    public LogService() throws Exception {
        FileUtil.crearDirectoriosParaArchivos(RUTA_LOG);
    }

    /**
     * Registra una acción en el log usando RandomAccessFile
     */
    public void registrarLog(LogEntry.Modulo modulo, LogEntry.Accion accion, String detalle, String idEntidad) {
        try {
            LogEntry entry = new LogEntry();
            entry.setFechaHora(LocalDateTime.now());
            entry.setUsuario(usuarioActual);
            entry.setModulo(modulo);
            entry.setAccion(accion);
            entry.setDetalle(detalle);
            entry.setIdEntidad(idEntidad);

            //* Escribir en archivo usando RandomAccessFile
            try (RandomAccessFile raf = new RandomAccessFile(RUTA_LOG, "rw")) {
                //* Posicionarse al final del archivo
                raf.seek(raf.length());

                //* Escribir la línea de log con tamaño fijo
                String lineaLog = entry.toFileString();
                RandomAccessFileUtil.escribirCadenaFija(raf, lineaLog, LONGITUD_MAXIMA_LOG);
            }
        } catch (Exception e) {
            System.err.println("Error al escribir log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtiene todos los logs usando RandomAccessFile
     */
    public List<LogEntry> obtenerTodosLosLogs() throws ServiceException {
        try {
            List<LogEntry> logs = new ArrayList<>();

            try (RandomAccessFile raf = new RandomAccessFile(RUTA_LOG, "r")) {
                long cantidadRegistros = raf.length() / (LONGITUD_MAXIMA_LOG * 2); // *2 por writeChars

                for (int i = 0; i < cantidadRegistros; i++) {
                    raf.seek((long) i * (LONGITUD_MAXIMA_LOG * 2));
                    String linea = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_MAXIMA_LOG);

                    if (linea != null && !linea.trim().isEmpty()) {
                        LogEntry entry = LogEntry.fromFileString(linea.trim());
                        if (entry != null) {
                            logs.add(entry);
                        }
                    }
                }
            }

            return logs;
        } catch (Exception e) {
            throw new ServiceException("Error al leer logs: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene logs por fecha
     */
    public List<LogEntry> obtenerLogsPorFecha(LocalDate fecha) throws ServiceException {
        List<LogEntry> todos = obtenerTodosLosLogs();
        return todos.stream()
                .filter(log -> log.getFechaHora().toLocalDate().equals(fecha))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene logs por rango de fechas
     */
    public List<LogEntry> obtenerLogsPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) throws ServiceException {
        List<LogEntry> todos = obtenerTodosLosLogs();
        return todos.stream()
                .filter(log -> {
                    LocalDate fechaLog = log.getFechaHora().toLocalDate();
                    return !fechaLog.isBefore(fechaInicio) && !fechaLog.isAfter(fechaFin);
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene logs por módulo
     */
    public List<LogEntry> obtenerLogsPorModulo(LogEntry.Modulo modulo) throws ServiceException {
        List<LogEntry> todos = obtenerTodosLosLogs();
        return todos.stream()
                .filter(log -> log.getModulo() == modulo)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene logs por acción
     */
    public List<LogEntry> obtenerLogsPorAccion(LogEntry.Accion accion) throws ServiceException {
        List<LogEntry> todos = obtenerTodosLosLogs();
        return todos.stream()
                .filter(log -> log.getAccion() == accion)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene logs por usuario
     */
    public List<LogEntry> obtenerLogsPorUsuario(String usuario) throws ServiceException {
        List<LogEntry> todos = obtenerTodosLosLogs();
        return todos.stream()
                .filter(log -> usuario.equals(log.getUsuario()))
                .collect(Collectors.toList());
    }

    /**
     * Exporta logs a CSV usando RandomAccessFile
     */
    public void exportarLogsACSV(String rutaCSV) throws ServiceException {
        try {
            List<LogEntry> logs = obtenerTodosLosLogs();

            //* Usar RandomAccessFile para escribir CSV
            try (RandomAccessFile raf = new RandomAccessFile(rutaCSV, "rw")) {
                //* Escribir cabecera
                String cabecera = "Fecha,Hora,Usuario,Modulo,Accion,Detalle,ID\n";
                raf.write(cabecera.getBytes());

                //* Escribir datos
                for (LogEntry log : logs) {
                    String linea = String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            log.getFechaHora().toLocalDate().toString(),
                            log.getFechaHora().toLocalTime().toString(),
                            log.getUsuario() != null ? log.getUsuario() : "SISTEMA",
                            log.getModulo(),
                            log.getAccion(),
                            log.getDetalle() != null ? log.getDetalle().replace(",", ";") : "",
                            log.getIdEntidad() != null ? log.getIdEntidad() : ""
                    );
                    raf.write(linea.getBytes());
                }
            }
        } catch (Exception e) {
            throw new ServiceException("Error al exportar logs a CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Limpia logs antiguos usando RandomAccessFile
     */
    public void limpiarLogsAntiguos(int dias) throws ServiceException {
        try {
            List<LogEntry> logs = obtenerTodosLosLogs();
            LocalDateTime limite = LocalDateTime.now().minusDays(dias);

            List<LogEntry> logsRecientes = logs.stream()
                    .filter(log -> log.getFechaHora().isAfter(limite))
                    .collect(Collectors.toList());

            //* Reescribir archivo solo con logs recientes usando RandomAccessFile
            try (RandomAccessFile raf = new RandomAccessFile(RUTA_LOG, "rw")) {
                //* Truncar el archivo
                raf.setLength(0);

                //* Escribir logs recientes
                for (LogEntry log : logsRecientes) {
                    String lineaLog = log.toFileString();
                    RandomAccessFileUtil.escribirCadenaFija(raf, lineaLog, LONGITUD_MAXIMA_LOG);
                }
            }
        } catch (Exception e) {
            throw new ServiceException("Error al limpiar logs: " + e.getMessage(), e);
        }
    }
}