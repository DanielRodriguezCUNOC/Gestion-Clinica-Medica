package com.paboomi.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicoDTO {


    private String id;
    private String nombresCompletos;
    private String especialidad;
    private String telefono;
    private String correo;
    private String horarioInicioAtencion;
    private String horarioFinAtencion;
    private String estado;
}
