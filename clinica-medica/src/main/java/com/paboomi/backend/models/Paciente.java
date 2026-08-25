package com.paboomi.backend.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class Paciente {

    private String identificacion;
    private String nombres;
    private String apellidos;
    private Date fechaNacimiento;
    private char sexo;
    private String numeroTelefono;
    private String email;
    private String tipoSangre;

    //* Para escritura/lectura de paciente
    public Paciente(String identificacion, String nombres, String apellidos, Date fechaNacimiento, char sexo, String numeroTelefono, String email, String tipoSangre) {
        this.identificacion = identificacion;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.numeroTelefono = numeroTelefono;
        this.email = email;
        this.tipoSangre = tipoSangre;
    }
}
