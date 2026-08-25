package com.paboomi.frontend.facade;

import com.paboomi.backend.dto.*;
import com.paboomi.backend.services.CitaService;
import com.paboomi.backend.services.LogService;
import com.paboomi.backend.services.MedicoService;
import com.paboomi.backend.services.PacienteService;
import com.paboomi.backend.util.exceptions.ServiceException;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ClinicaFacade {

    private static ClinicaFacade instance;
    private MedicoService medicoService;
    private PacienteService pacienteService;
    private CitaService citaService;
    private LogService logService;

    public ClinicaFacade() {
        instanciarServicios();
    }

    private void instanciarServicios(){
        try {
            this.logService = new LogService();
            this.citaService = new CitaService(logService);
            this.medicoService = new MedicoService(citaService, logService);
            this.pacienteService = new PacienteService(citaService, logService);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao instanciar servicos");
        }
    }

    public static synchronized ClinicaFacade getInstance() {
        if (instance == null) instance = new ClinicaFacade();
        return instance;
    }

    //* Módulo de Autenticación
    public boolean iniciarSesion(String usuario, String password) { return false; }

    //* --- Métodos Médicos ---
    public void registrarPaciente(RegistrarPacienteDTO dto) throws ServiceException { pacienteService.registrarPaciente(dto); }
    public List<PacienteDTO> listarPacientes() throws ServiceException { return pacienteService.listarTodos(); }
    public PacienteDTO buscarPacientePorIdentificacion(String id) throws ServiceException { return pacienteService.buscarPorIdentificacion(id); }
    public List<PacienteDTO> buscarPacientesPorNombre(String nombre) throws ServiceException { return pacienteService.buscarPorNombre(nombre); }
    public List<PacienteDTO> obtenerPacientesConMasCitas() throws  ServiceException {return pacienteService.obtenerPacientesConMasCitas();}

    public void eliminarPaciente(String id) throws ServiceException { pacienteService.eliminarPaciente(id); }

    //* --- Métodos Médicos ---
    public void registrarMedico(RegistrarMedicoDTO dto) throws Exception { medicoService.registrarMedico(dto); }
    public List<MedicoDTO> listarMedicos() throws ServiceException { return medicoService.listarTodos(); }
    public List<CitaDTO> buscarCitasPorMedico(UUID idMedico) throws ServiceException {return citaService.buscarPorMedico(idMedico);}

    //* --- Métodos Citas ---
    public void programarCita(RegistrarCitaDTO dto) throws Exception { citaService.programarCita(dto); }
    public List<CitaDTO> listarCitas() throws ServiceException { return citaService.listarTodos(); }

    }