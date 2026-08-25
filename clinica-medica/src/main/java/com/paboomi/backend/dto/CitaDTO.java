package com.paboomi.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaDTO {
    private String id;
    private String pacienteIdentificacion;
    private String pacienteNombre;
    private String medicoId;
    private String medicoNombre;
    private String fecha;
    private String horaInicio;
    private String motivo;
    private String estado;
    private String observaciones;
}