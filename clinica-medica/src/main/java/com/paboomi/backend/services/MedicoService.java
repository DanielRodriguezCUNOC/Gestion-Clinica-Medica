package com.paboomi.backend.services;

import com.paboomi.backend.dao.MedicoDAO;
import com.paboomi.backend.dto.CitaDTO;
import com.paboomi.backend.dto.MedicoDTO;
import com.paboomi.backend.dto.RegistrarMedicoDTO;
import com.paboomi.backend.models.LogEntry;
import com.paboomi.backend.models.Medico;
import com.paboomi.backend.util.exceptions.ServiceException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class MedicoService {

    private final MedicoDAO medicoDAO;
    private final CitaService citaService;
    private final LogService logService;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public MedicoService(CitaService citaService, LogService logService) throws Exception {
        this.medicoDAO = new MedicoDAO();
        this.citaService = citaService;
        this.logService = logService;
    }

    //* MÉTODOS DE NEGOCIO

    //* Registra un nuevo médico en el sistema
    public void registrarMedico(RegistrarMedicoDTO dto) throws ServiceException {
        try {
            //* Validaciones de negocio
            validarDatosMedico(dto);

            //* Validar horarios
            LocalTime horaInicio = LocalTime.parse(dto.getHorarioInicio(), TIME_FORMATTER);
            LocalTime horaFin = LocalTime.parse(dto.getHorarioFin(), TIME_FORMATTER);

            if (!horaFin.isAfter(horaInicio)) {
                throw new ServiceException("El horario de fin debe ser posterior al horario de inicio");
            }

            //* Crear la entidad
            Medico medico = new Medico(
                    dto.getNombres(),
                    dto.getApellidos(),
                    dto.getEspecialidad(),
                    dto.getTelefono(),
                    dto.getCorreo(),
                    horaInicio,
                    horaFin
            );

            //* Guardar
            medicoDAO.registrarMedico(medico);

            //* Regitrar en el log
            logService.registrarLog(
                    LogEntry.Modulo.MEDICOS,
                    LogEntry.Accion.CREACIÓN,
                    "Médico registrado: " + medico.getNombres() + " " + medico.getApellidos() +
                            " | Especialidad: " + medico.getEspecialidad() +
                            " | Horario: " + medico.getHorarioInicio().format(TIME_FORMATTER) + " - " + medico.getHorarioFin().format(TIME_FORMATTER),
                    medico.getId().toString()
            );

        } catch (DateTimeParseException e) {
            throw new ServiceException("Formato de hora inválido. Use HH:mm");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al registrar médico: " + e.getMessage(), e);
        }
    }

    /**
     * Busca un médico por su UUID
     */
    public MedicoDTO buscarPorId(UUID id) throws ServiceException {
        try {
            Medico medico = medicoDAO.buscarPorId(id);
            if (medico == null) {
                return null;
            }
            return convertirADTO(medico);
        } catch (Exception e) {
            throw new ServiceException("Error al buscar médico: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todos los médicos
     */
    public List<MedicoDTO> listarTodos() throws ServiceException {
        try {
            List<Medico> medicos = medicoDAO.listarTodos();
            return medicos.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al listar médicos: " + e.getMessage(), e);
        }
    }

    /**
     * Busca médicos por nombre (coincidencia parcial)
     */
    public List<MedicoDTO> buscarPorNombre(String nombre) throws ServiceException {
        try {
            if (nombre == null || nombre.trim().isEmpty()) {
                return listarTodos();
            }
            List<Medico> medicos = medicoDAO.buscarPorNombre(nombre.trim());
            return medicos.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al buscar médicos por nombre: " + e.getMessage(), e);
        }
    }

    /**
     * Busca médicos por especialidad
     */
    public List<MedicoDTO> buscarPorEspecialidad(String especialidad) throws ServiceException {
        try {
            if (especialidad == null || especialidad.trim().isEmpty()) {
                return listarTodos();
            }
            List<Medico> medicos = medicoDAO.buscarPorEspecialidad(especialidad.trim());
            return medicos.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al buscar médicos por especialidad: " + e.getMessage(), e);
        }
    }

    /**
     * Lista médicos activos
     */
    public List<MedicoDTO> listarActivos() throws ServiceException {
        try {
            List<Medico> medicos = medicoDAO.listarActivos();
            return medicos.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al listar médicos activos: " + e.getMessage(), e);
        }
    }

    /**
     * Lista médicos inactivos
     */
    public List<MedicoDTO> listarInactivos() throws ServiceException {
        try {
            List<Medico> medicos = medicoDAO.listarInactivos();
            return medicos.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al listar médicos inactivos: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza la información de un médico existente
     */
    public void actualizarMedico(UUID id, RegistrarMedicoDTO dto) throws ServiceException {
        try {
            //* Verificar que existe
            Medico medicoExistente = medicoDAO.buscarPorId(id);
            if (medicoExistente == null) {
                throw new ServiceException("Médico no encontrado con ID: " + id);
            }

            //* Validaciones
            validarDatosMedico(dto);

            //* Validar horarios
            LocalTime horaInicio = LocalTime.parse(dto.getHorarioInicio(), TIME_FORMATTER);
            LocalTime horaFin = LocalTime.parse(dto.getHorarioFin(), TIME_FORMATTER);

            if (!horaFin.isAfter(horaInicio)) {
                throw new ServiceException("El horario de fin debe ser posterior al horario de inicio");
            }

            try {
                //* Obtener todas las citas del médico
                List<CitaDTO> citasMedico = citaService.buscarPorMedico(id);

                //* Verificar si hay citas fuera del nuevo horario
                boolean hayConflicto = citasMedico.stream()
                        .filter(cita -> !cita.getEstado().equals("Cancelada") && !cita.getEstado().equals("Atendida"))
                        .anyMatch(cita -> {
                            LocalTime horaCita = LocalTime.parse(cita.getHoraInicio());
                            return horaCita.isBefore(horaInicio) || horaCita.isAfter(horaFin);
                        });

                if (hayConflicto) {
                    throw new ServiceException(
                            "No se puede cambiar el horario porque el médico tiene citas programadas " +
                                    "fuera del nuevo rango horario (" + horaInicio.format(TIME_FORMATTER) +
                                    " - " + horaFin.format(TIME_FORMATTER) + ")"
                    );
                }

            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                throw new ServiceException("Error al verificar citas del médico: " + e.getMessage(), e);
            }

            //* Actualizar datos
            medicoExistente.setNombres(dto.getNombres());
            medicoExistente.setApellidos(dto.getApellidos());
            medicoExistente.setEspecialidad(dto.getEspecialidad());
            medicoExistente.setTelefono(dto.getTelefono());
            medicoExistente.setCorreoElectronico(dto.getCorreo());
            medicoExistente.setHorarioInicio(horaInicio);
            medicoExistente.setHorarioFin(horaFin);

            //* Guardar cambios
            medicoDAO.actualizarMedico(medicoExistente);

            //* Regitrar en el log
            logService.registrarLog(
                    LogEntry.Modulo.MEDICOS,
                    LogEntry.Accion.ACTUALIZACIÓN,
                    "Médico actualizado: " + medicoExistente.getNombres() + " " + medicoExistente.getApellidos() +
                            " | Especialidad: " + medicoExistente.getEspecialidad(),
                    medicoExistente.getId().toString()
            );

        } catch (DateTimeParseException e) {
            throw new ServiceException("Formato de hora inválido. Use HH:mm");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al actualizar médico: " + e.getMessage(), e);
        }
    }

    /**
     * Activa un médico
     */
    public void activarMedico(UUID id) throws ServiceException {
        try {
            Medico medico = medicoDAO.buscarPorId(id);
            if (medico == null) {
                throw new ServiceException("Médico no encontrado con ID: " + id);
            }
            if (medico.isActivo()) {
                throw new ServiceException("El médico ya está activo");
            }
            medicoDAO.activarMedico(id);

            logService.registrarLog(
                    LogEntry.Modulo.MEDICOS,
                    LogEntry.Accion.ACTIVACIÓN,
                    "Médico activado: " + medico.getNombres() + " " + medico.getApellidos() +
                            " | Especialidad: " + medico.getEspecialidad(),
                    id.toString()
            );
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al activar médico: " + e.getMessage(), e);
        }
    }

    /**
     * Desactiva un médico
     */
    public void desactivarMedico(UUID id) throws ServiceException {
        try {
            Medico medico = medicoDAO.buscarPorId(id);
            if (medico == null) {
                throw new ServiceException("Médico no encontrado con ID: " + id);
            }
            if (!medico.isActivo()) {
                throw new ServiceException("El médico ya está inactivo");
            }

            int cantidadEspecialistas = obtenerMedicosActivosPorEspecialidad(medico.getEspecialidad()).size();
            if (cantidadEspecialistas > 0) {
                medicoDAO.desactivarMedico(id);
            }else {
                throw new ServiceException("No es posible inactivar al único medico con la espcialidad: " + medico.getEspecialidad());
            }


            medicoDAO.desactivarMedico(id);
            logService.registrarLog(
                    LogEntry.Modulo.MEDICOS,
                    LogEntry.Accion.DESACTIVACIÓN,
                    "Médico desactivado: " + medico.getNombres() + " " + medico.getApellidos() +
                            " | Especialidad: " + medico.getEspecialidad(),
                    id.toString()
            );
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al desactivar médico: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un médico (soft delete)
     */
    public void eliminarMedico(UUID id) throws ServiceException {
        try {
            Medico medico = medicoDAO.buscarPorId(id);
            if (medico == null) {
                throw new ServiceException("Médico no encontrado con ID: " + id);
            }

            medicoDAO.eliminarMedico(id);

            //* Regitrar en el log
            logService.registrarLog(
                    LogEntry.Modulo.MEDICOS,
                    LogEntry.Accion.ELIMINACIÓN,
                    "Médico eliminado: " + medico.getNombres() + " " + medico.getApellidos() +
                            " | Especialidad: " + medico.getEspecialidad(),
                    medico.getId().toString()
            );

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al eliminar médico: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si un médico existe
     */
    public boolean existeMedico(UUID id) throws ServiceException {
        try {
            return medicoDAO.existeMedico(id);
        } catch (Exception e) {
            throw new ServiceException("Error al verificar existencia del médico: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene todas las especialidades disponibles
     */
    public List<String> obtenerEspecialidades() throws ServiceException {
        try {
            return medicoDAO.obtenerEspecialidades().stream().sorted().collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al obtener especialidades: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene médicos activos por especialidad
     */
    public List<MedicoDTO> obtenerMedicosActivosPorEspecialidad(String especialidad) throws ServiceException {
        try {
            List<Medico> medicos = medicoDAO.buscarPorEspecialidad(especialidad);
            return medicos.stream()
                    .filter(Medico::isActivo)
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al obtener médicos por especialidad: " + e.getMessage(), e);
        }
    }

    //* MÉTODOS PRIVADOS

    /**
     * Convierte un Medico a MedicoDTO
     */
    private MedicoDTO convertirADTO(Medico medico) {
        MedicoDTO dto = new MedicoDTO();
        dto.setId(medico.getId().toString());
        dto.setNombresCompletos(medico.getNombres() + " " + medico.getApellidos());
        dto.setNombresCompletos(medico.getNombres() + " " + medico.getApellidos());
        dto.setEspecialidad(medico.getEspecialidad());
        dto.setTelefono(medico.getTelefono());
        dto.setCorreo(medico.getCorreoElectronico());
        dto.setHorarioInicioAtencion(medico.getHorarioInicio().format(TIME_FORMATTER));
        dto.setHorarioFinAtencion(medico.getHorarioFin().format(TIME_FORMATTER));
        dto.setEstado(medico.isActivo() ? "Activo" : "Inactivo");
        return dto;
    }

    /**
     * Valida los datos de un médico
     */
    private void validarDatosMedico(RegistrarMedicoDTO dto) throws ServiceException {
        if (dto == null) {
            throw new ServiceException("Los datos del médico no pueden ser nulos");
        }

        if (dto.getNombres() == null || dto.getNombres().trim().isEmpty()) {
            throw new ServiceException("Los nombres son obligatorios");
        }

        if (dto.getApellidos() == null || dto.getApellidos().trim().isEmpty()) {
            throw new ServiceException("Los apellidos son obligatorios");
        }

        if (dto.getEspecialidad() == null || dto.getEspecialidad().trim().isEmpty()) {
            throw new ServiceException("La especialidad es obligatoria");
        }

        if (dto.getTelefono() == null || dto.getTelefono().trim().isEmpty()) {
            throw new ServiceException("El teléfono es obligatorio");
        }

        // Validar formato de teléfono (opcional)
        if (!dto.getTelefono().matches("^[0-9\\-\\+\\s]{8,15}$")) {
            throw new ServiceException("Formato de teléfono inválido");
        }

        // Validar formato de correo (opcional)
        if (dto.getCorreo() != null && !dto.getCorreo().isEmpty()) {
            if (!dto.getCorreo().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new ServiceException("Formato de correo electrónico inválido");
            }
        }

        // Validar horarios
        if (dto.getHorarioInicio() == null || dto.getHorarioInicio().trim().isEmpty()) {
            throw new ServiceException("El horario de inicio es obligatorio");
        }

        if (dto.getHorarioFin() == null || dto.getHorarioFin().trim().isEmpty()) {
            throw new ServiceException("El horario de fin es obligatorio");
        }
    }
}