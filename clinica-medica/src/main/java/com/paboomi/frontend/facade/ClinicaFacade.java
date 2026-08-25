package com.paboomi.frontend.facade;

import com.paboomi.backend.dto.*;
import com.paboomi.backend.services.CitaService;
import com.paboomi.backend.services.MedicoService;
import com.paboomi.backend.services.PacienteService;
import com.paboomi.backend.util.exceptions.ServiceException;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClinicaFacade {

    private static ClinicaFacade instance;
    private MedicoService medicoService;
    private PacienteService pacienteService;
    private CitaService citaService;

    public ClinicaFacade() {
        instanciarServicios();
    }

    private void instanciarServicios(){
        try {
            this.citaService = new CitaService();
            this.medicoService = new MedicoService(citaService);
            this.pacienteService = new PacienteService(citaService);
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

    public void registrarPaciente(RegistrarPacienteDTO dto) throws ServiceException { pacienteService.registrarPaciente(dto); }
    public List<PacienteDTO> listarPacientes() throws ServiceException { return pacienteService.listarTodos(); }
    public PacienteDTO buscarPacientePorIdentificacion(String id) throws ServiceException { return pacienteService.buscarPorIdentificacion(id); }
    public List<PacienteDTO> buscarPacientesPorNombre(String nombre) throws ServiceException { return pacienteService.buscarPorNombre(nombre); }
    public void eliminarPaciente(String id) throws ServiceException { pacienteService.eliminarPaciente(id); }
    // --- Métodos Médicos ---
    public void registrarMedico(RegistrarMedicoDTO dto) throws Exception { medicoService.registrarMedico(dto); }
    public List<MedicoDTO> listarMedicos() throws ServiceException { return medicoService.listarTodos(); }

    // --- Métodos Citas ---
    public void programarCita(RegistrarCitaDTO dto) throws Exception { citaService.programarCita(dto); }
    public List<CitaDTO> listarCitas() throws ServiceException { return citaService.listarTodos(); }

    }