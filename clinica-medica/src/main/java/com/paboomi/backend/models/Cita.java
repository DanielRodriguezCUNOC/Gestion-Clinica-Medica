package com.paboomi.backend.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class Cita {

    private UUID id;
    private String idPaciente;
    private UUID idMedico;
    private LocalDate fechaCita;
    private LocalTime horaInicio;
    private String motivo;
    private String estado;
    private String observaciones;

    public Cita(String idPaciente, UUID idMedico, LocalDate fechaCita, LocalTime horaInicio, String motivo, String estado, String observaciones) {
        this.id = UUID.randomUUID();
        this.idPaciente = idPaciente;
        this.idMedico = idMedico;
        this.fechaCita = fechaCita;
        this.horaInicio = horaInicio;
        this.motivo = motivo;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    public Cita(UUID id, String idPaciente, UUID idMedico, LocalDate fechaCita, LocalTime horaInicio, String motivo, String estado, String observaciones) {
        this.id = id;
        this.idPaciente = idPaciente;
        this.idMedico = idMedico;
        this.fechaCita = fechaCita;
        this.horaInicio = horaInicio;
        this.motivo = motivo;
        this.estado = estado;
        this.observaciones = observaciones;
    }
}
