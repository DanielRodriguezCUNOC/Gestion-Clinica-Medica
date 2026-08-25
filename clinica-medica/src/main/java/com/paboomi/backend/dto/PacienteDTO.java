package com.paboomi.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDTO {
    private String identificacion;
    private String nombresCompletos;
    private String nombres;
    private String apellidos;
    private String fechaNacimiento;
    private String sexo;
    private String telefono;
    private String email;
    private String tipoSangre;
}