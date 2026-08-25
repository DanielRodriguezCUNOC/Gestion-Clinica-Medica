package com.paboomi.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarMedicoDTO {
    private String nombres;
    private String apellidos;
    private String especialidad;
    private String telefono;
    private String correo;
    private String horarioInicio;
    private String horarioFin;
}