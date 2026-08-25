package com.paboomi.frontend.facade;

import com.paboomi.backend.models.Cita;
import com.paboomi.backend.models.Medico;
import com.paboomi.backend.models.Paciente;

import java.util.Date;
import java.util.List;

public class ClinicaFacade {

    //* Módulo de Autenticación
    public boolean iniciarSesion(String usuario, String password) { return false; }

    //* Módulo de Pacientes
    public boolean registrarPaciente(Paciente paciente) throws Exception { return false; }
    public List<Paciente> obtenerTodosPacientes() {return null;  }
    public List<Paciente> buscarPacientes(String identificacion, String nombre, String apellido) {return null; }
    public boolean modificarPaciente(Paciente paciente) throws Exception { return false; }
    public boolean eliminarPaciente(String identificacion) { return false; }

    //* Módulo de Médicos
    public boolean registrarMedico(Medico medico) throws Exception { return false; }
    public List<Medico> buscarMedicos(String uuid, String nombre, String apellido, String especialidad) {return null; }
    public boolean cambiarEstadoMedico(String uuid, boolean activo) { return false;}
    public List<Medico> filtrarMedicosPorEstado(boolean activos) { return null;}

    //* Módulo de Citas
    public boolean programarCita(Cita cita) throws Exception { return false;}
    public List<Cita> buscarCitas(String uuidPaciente, String uuidMedico, Date fecha, String estado) {return null;}
    public boolean cambiarEstadoCita(String uuidCita, String nuevoEstado) {return false; }
    public boolean modificarDetallesCita(String uuidCita, String motivo, String observaciones) { return false; }
}