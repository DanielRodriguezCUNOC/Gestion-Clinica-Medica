package com.paboomi.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarPacienteDTO {
    private String identificacion;
    private String nombres;
    private String apellidos;
    private String fechaNacimiento; //* yyyy-MM-dd
    private String sexo; //* M/F
    private String telefono;
    private String email;
    private String tipoSangre;
}