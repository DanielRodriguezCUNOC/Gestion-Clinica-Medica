package com.paboomi.backend.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {

    public enum Modulo {
        MEDICOS, PACIENTES, CITAS, REPORTES, SISTEMA
    }

    public enum Accion {
        CREACIÓN, ACTUALIZACIÓN, ELIMINACIÓN, CONSULTA, ACTIVACIÓN, DESACTIVACIÓN,
        CANCELACIÓN, ATENCIÓN, LOGIN, LOGOUT, EXPORTACIÓN
    }

    private LocalDateTime fechaHora;
    private String usuario;
    private Modulo modulo;
    private Accion accion;
    private String detalle;
    private String idEntidad;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String toFileString() {
        return String.format("[%s] | USUARIO: %s | MODULO: %s | ACCION: %s | DETALLE: %s | ID: %s",
                fechaHora.format(FORMATTER),
                usuario != null ? usuario : "SISTEMA",
                modulo,
                accion,
                detalle != null ? detalle : "",
                idEntidad != null ? idEntidad : ""
        );
    }

    public static LogEntry fromFileString(String line) {
        try {

            String[] parts = line.split(" \\| ");
            LogEntry entry = new LogEntry();

            String fechaPart = parts[0].replace("[", "").replace("]", "");
            entry.setFechaHora(LocalDateTime.parse(fechaPart, FORMATTER));

            // Usuario
            String usuarioPart = parts[1].replace("USUARIO: ", "");
            entry.setUsuario(usuarioPart.equals("SISTEMA") ? null : usuarioPart);

            // Módulo
            String moduloPart = parts[2].replace("MODULO: ", "");
            entry.setModulo(Modulo.valueOf(moduloPart.toUpperCase()));

            // Acción
            String accionPart = parts[3].replace("ACCION: ", "");
            entry.setAccion(Accion.valueOf(accionPart.toUpperCase()));

            // Detalle
            String detallePart = parts[4].replace("DETALLE: ", "");
            entry.setDetalle(detallePart);

            // ID
            if (parts.length > 5) {
                String idPart = parts[5].replace("ID: ", "");
                entry.setIdEntidad(idPart);
            }

            return entry;
        } catch (Exception e) {
            return null;
        }
    }
}