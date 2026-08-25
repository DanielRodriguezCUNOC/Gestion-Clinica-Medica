package com.paboomi.backend.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class Medico {

    private UUID id;
    private String nombres;
    private String apellidos;
    private String especialidad;
    private String telefono;
    private String correoElectronico;
    private LocalTime horarioInicio;
    private LocalTime horarioFin;
    private boolean activo;


    //* Constructor para la creación de un nuevo Medico
    public Medico(String nombres, String apellidos, String especialidad, String telefono,
                  String correoElectronico, LocalTime horarioInicio, LocalTime horarioFin) {

        this.id = UUID.randomUUID();
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.especialidad = especialidad;
        this.telefono = telefono;
        this.correoElectronico = correoElectronico;
        this.horarioInicio = horarioInicio;
        this.horarioFin = horarioFin;
        this.activo = true;
    }

    //* Constructor para lectura de archivos (cuando ya existe el UUID)
    public Medico(UUID id, String nombres, String apellidos, String especialidad, String telefono,
                  String correoElectronico, LocalTime horarioInicio, LocalTime horarioFin, boolean activo) {
        this.id = id;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.especialidad = especialidad;
        this.telefono = telefono;
        this.correoElectronico = correoElectronico;
        this.horarioInicio = horarioInicio;
        this.horarioFin = horarioFin;
        this.activo = activo;
    }

}
