package com.paboomi.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarCitaDTO {
    private String identificacionPaciente;
    private String idMedico;
    private String fecha;
    private String horaInicio;
    private String estado;
    private String motivo;
    private String observaciones;
}