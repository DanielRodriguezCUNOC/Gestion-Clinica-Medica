package com.paboomi.backend.services;

import com.paboomi.backend.dao.CitaDAO;
import com.paboomi.backend.dao.MedicoDAO;
import com.paboomi.backend.dao.PacienteDAO;
import com.paboomi.backend.dto.CitaDTO;
import com.paboomi.backend.dto.RegistrarCitaDTO;
import com.paboomi.backend.models.Cita;
import com.paboomi.backend.models.Medico;
import com.paboomi.backend.models.Paciente;
import com.paboomi.backend.util.exceptions.ServiceException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class CitaService {

    private final CitaDAO citaDAO;
    private final MedicoDAO medicoDAO;
    private final PacienteDAO pacienteDAO;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public CitaService() throws Exception {
        this.citaDAO = new CitaDAO();
        this.medicoDAO = new MedicoDAO();
        this.pacienteDAO = new PacienteDAO();
    }

    // ============ MÉTODOS DE NEGOCIO ============

    /**
     * Programa una nueva cita
     */
    public void programarCita(RegistrarCitaDTO dto) throws ServiceException {
        try {
            //* Validaciones básicas
            validarDatosCita(dto);

            //! Verificar que el paciente existe
            Paciente paciente = pacienteDAO.buscarPorIdentificacion(dto.getIdentificacionPaciente());
            if (paciente == null) {
                throw new ServiceException("Paciente no encontrado con identificación: " + dto.getIdentificacionPaciente());
            }

            //! Verificar que el médico existe y está activo
            UUID idMedico = UUID.fromString(dto.getIdMedico());
            Medico medico = medicoDAO.buscarPorId(idMedico);
            if (medico == null) {
                throw new ServiceException("Médico no encontrado con ID: " + dto.getIdMedico());
            }
            if (!medico.isActivo()) {
                throw new ServiceException("El médico no está disponible");
            }

            //! Verificar que la fecha no sea pasada
            LocalDate fechaCita = LocalDate.parse(dto.getFecha(), DATE_FORMATTER);
            if (fechaCita.isBefore(LocalDate.now())) {
                throw new ServiceException("No se pueden programar citas en fechas pasadas");
            }

            //! Verificar que la hora esté dentro del horario del médico
            LocalTime horaCita = LocalTime.parse(dto.getHoraInicio(), TIME_FORMATTER);
            if (horaCita.isBefore(medico.getHorarioInicio()) || horaCita.isAfter(medico.getHorarioFin())) {
                throw new ServiceException(
                        "La hora de la cita (" + horaCita.format(TIME_FORMATTER) +
                                ") está fuera del horario de atención del médico (" +
                                medico.getHorarioInicio().format(TIME_FORMATTER) + " - " +
                                medico.getHorarioFin().format(TIME_FORMATTER) + ")"
                );
            }

            //! Verificar que el médico no tenga otra cita a la misma hora
            List<Cita> citasMedico = citaDAO.buscarPorMedico(idMedico);
            boolean conflictoHorario = citasMedico.stream()
                    .filter(c -> c.getFechaCita().equals(fechaCita))
                    .filter(c -> !c.getEstado().equals("Cancelada"))
                    .anyMatch(c -> c.getHoraInicio().equals(horaCita));

            if (conflictoHorario) {
                throw new ServiceException(
                        "El médico ya tiene una cita programada para el " +
                                fechaCita.format(DATE_FORMATTER) + " a las " +
                                horaCita.format(TIME_FORMATTER)
                );
            }

            //* Crear la cita
            Cita cita = new Cita(
                    dto.getIdentificacionPaciente(),
                    idMedico,
                    fechaCita,
                    horaCita,
                    dto.getMotivo(),
                    dto.getEstado(),
                    dto.getObservaciones()
            );

            // Guardar
            citaDAO.registrarCita(cita);

        } catch (DateTimeParseException e) {
            throw new ServiceException("Formato de fecha u hora inválido. Use yyyy-MM-dd y HH:mm");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al programar cita: " + e.getMessage(), e);
        }
    }

    /**
     * Busca una cita por su UUID
     */
    public CitaDTO buscarPorId(UUID id) throws ServiceException {
        try {
            Cita cita = citaDAO.buscarPorId(id);
            if (cita == null) {
                return null;
            }
            return convertirADTO(cita);
        } catch (Exception e) {
            throw new ServiceException("Error al buscar cita: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todas las citas
     */
    public List<CitaDTO> listarTodos() throws ServiceException {
        try {
            List<Cita> citas = citaDAO.listarTodos();
            return citas.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al listar citas: " + e.getMessage(), e);
        }
    }

    /**
     * Busca citas por paciente
     */
    public List<CitaDTO> buscarPorPaciente(String identificacionPaciente) throws ServiceException {
        try {
            if (identificacionPaciente == null || identificacionPaciente.trim().isEmpty()) {
                throw new ServiceException("La identificación del paciente es obligatoria");
            }

            //* Verificar que el paciente existe
            Paciente paciente = pacienteDAO.buscarPorIdentificacion(identificacionPaciente);
            if (paciente == null) {
                throw new ServiceException("Paciente no encontrado con identificación: " + identificacionPaciente);
            }

            List<Cita> citas = citaDAO.buscarPorPaciente(identificacionPaciente);
            return citas.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al buscar citas por paciente: " + e.getMessage(), e);
        }
    }

    /**
     * Busca citas por médico
     */
    public List<CitaDTO> buscarPorMedico(UUID idMedico) throws ServiceException {
        try {
            if (idMedico == null) {
                throw new ServiceException("El ID del médico es obligatorio");
            }

            //* Verificar que el médico existe
            Medico medico = medicoDAO.buscarPorId(idMedico);
            if (medico == null) {
                throw new ServiceException("Médico no encontrado con ID: " + idMedico);
            }

            List<Cita> citas = citaDAO.buscarPorMedico(idMedico);
            return citas.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al buscar citas por médico: " + e.getMessage(), e);
        }
    }

    /**
     * Busca citas por fecha
     */
    public List<CitaDTO> buscarPorFecha(LocalDate fecha) throws ServiceException {
        try {
            if (fecha == null) {
                throw new ServiceException("La fecha es obligatoria");
            }

            List<Cita> citas = citaDAO.buscarPorFecha(fecha);
            return citas.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al buscar citas por fecha: " + e.getMessage(), e);
        }
    }

    /**
     * Busca citas por rango de fechas
     */
    public List<CitaDTO> buscarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) throws ServiceException {
        try {
            if (fechaInicio == null || fechaFin == null) {
                throw new ServiceException("Las fechas de inicio y fin son obligatorias");
            }

            if (fechaInicio.isAfter(fechaFin)) {
                throw new ServiceException("La fecha de inicio no puede ser posterior a la fecha fin");
            }

            List<Cita> citas = citaDAO.buscarPorRangoFechas(fechaInicio, fechaFin);
            return citas.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al buscar citas por rango de fechas: " + e.getMessage(), e);
        }
    }

    /**
     * Busca citas por estado
     */
    public List<CitaDTO> buscarPorEstado(String estado) throws ServiceException {
        try {
            if (estado == null || estado.trim().isEmpty()) {
                throw new ServiceException("El estado es obligatorio");
            }

            // Validar estado permitido
            if (!estado.equals("Programada") && !estado.equals("Atendida") && !estado.equals("Cancelada")) {
                throw new ServiceException("Estado inválido. Estados permitidos: Programada, Atendida, Cancelada");
            }

            List<Cita> citas = citaDAO.buscarPorEstado(estado);
            return citas.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al buscar citas por estado: " + e.getMessage(), e);
        }
    }

    /**
     * Cancela una cita
     */
    public void cancelarCita(UUID id) throws ServiceException {
        try {
            Cita cita = citaDAO.buscarPorId(id);
            if (cita == null) {
                throw new ServiceException("Cita no encontrada con ID: " + id);
            }

            //* Validar que no esté ya cancelada
            if (cita.getEstado().equals("Cancelada")) {
                throw new ServiceException("La cita ya está cancelada");
            }

            //* Validar que no esté atendida
            if (cita.getEstado().equals("Atendida")) {
                throw new ServiceException("No se puede cancelar una cita que ya fue atendida");
            }

            citaDAO.cancelarCita(id);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al cancelar cita: " + e.getMessage(), e);
        }
    }

    /**
     * Marca una cita como atendida
     */
    public void marcarComoAtendida(UUID id) throws ServiceException {
        try {
            Cita cita = citaDAO.buscarPorId(id);
            if (cita == null) {
                throw new ServiceException("Cita no encontrada con ID: " + id);
            }

            //* Validar que no esté cancelada
            if (cita.getEstado().equals("Cancelada")) {
                throw new ServiceException("No se puede marcar como atendida una cita cancelada");
            }

            //* Validar que no esté ya atendida
            if (cita.getEstado().equals("Atendida")) {
                throw new ServiceException("La cita ya fue atendida");
            }

            citaDAO.marcarComoAtendida(id);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al marcar cita como atendida: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza motivo y observaciones de una cita
     */
    public void actualizarCita(UUID id, String motivo, String observaciones) throws ServiceException {
        try {
            Cita cita = citaDAO.buscarPorId(id);
            if (cita == null) {
                throw new ServiceException("Cita no encontrada con ID: " + id);
            }

            // No permitir actualizar citas canceladas o atendidas
            if (cita.getEstado().equals("Cancelada") || cita.getEstado().equals("Atendida")) {
                throw new ServiceException("No se puede modificar una cita " + cita.getEstado().toLowerCase());
            }

            if (motivo != null && !motivo.trim().isEmpty()) {
                cita.setMotivo(motivo.trim());
            }

            if (observaciones != null) {
                cita.setObservaciones(observaciones.trim().isEmpty() ? null : observaciones.trim());
            }

            citaDAO.actualizarCita(cita);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al actualizar cita: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina una cita (soft delete)
     */
    public void eliminarCita(UUID id) throws ServiceException {
        try {
            Cita cita = citaDAO.buscarPorId(id);
            if (cita == null) {
                throw new ServiceException("Cita no encontrada con ID: " + id);
            }

            //* Solo permitir eliminar citas programadas o canceladas
            if (cita.getEstado().equals("Atendida")) {
                throw new ServiceException("No se puede eliminar una cita atendida");
            }

            citaDAO.eliminarCita(id);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al eliminar cita: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si un médico tiene citas programadas
     */
    public boolean medicoTieneCitasProgramadas(UUID idMedico) throws ServiceException {
        try {
            List<Cita> citas = citaDAO.buscarPorMedico(idMedico);
            return citas.stream()
                    .anyMatch(c -> c.getEstado().equals("Programada"));
        } catch (Exception e) {
            throw new ServiceException("Error al verificar citas del médico: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el conteo de citas por médico (para reportes)
     */
    public long contarCitasPorMedico(UUID idMedico) throws ServiceException {
        try {
            List<Cita> citas = citaDAO.buscarPorMedico(idMedico);
            return citas.size();
        } catch (Exception e) {
            throw new ServiceException("Error al contar citas del médico: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el conteo de citas por paciente (para reportes)
     */
    public long contarCitasPorPaciente(String identificacionPaciente) throws ServiceException {
        try {
            List<Cita> citas = citaDAO.buscarPorPaciente(identificacionPaciente);
            return citas.size();
        } catch (Exception e) {
            throw new ServiceException("Error al contar citas del paciente: " + e.getMessage(), e);
        }
    }

    //* MÉTODOS PRIVADOS

    /**
     * Convierte un Cita a CitaDTO con información adicional del paciente y médico
     */
    private CitaDTO convertirADTO(Cita cita) {
        CitaDTO dto = new CitaDTO();
        dto.setId(cita.getId().toString());
        dto.setIdentificacionPaciente(cita.getIdPaciente());
        dto.setIdMedico(cita.getIdMedico().toString());
        dto.setFecha(cita.getFechaCita().format(DATE_FORMATTER));
        dto.setHoraInicio(cita.getHoraInicio().format(TIME_FORMATTER));
        dto.setMotivo(cita.getMotivo());
        dto.setEstado(cita.getEstado());
        dto.setObservaciones(cita.getObservaciones());

        //* Obtener nombres del paciente
        try {
            Paciente paciente = pacienteDAO.buscarPorIdentificacion(cita.getIdPaciente());
            if (paciente != null) {
                dto.setNombrePaciente(paciente.getNombres() + " " + paciente.getApellidos());
            }
        } catch (Exception e) {
        }

        //* Obtener nombre del médico
        try {
            Medico medico = medicoDAO.buscarPorId(cita.getIdMedico());
            if (medico != null) {
                dto.setNombreMedico(medico.getNombres() + " " + medico.getApellidos());
                dto.setEspecialidadMedico(medico.getEspecialidad());
            }
        } catch (Exception e) {
        }

        return dto;
    }

    /**
     * Valida los datos de una cita
     */
    private void validarDatosCita(RegistrarCitaDTO dto) throws ServiceException {
        if (dto == null) {
            throw new ServiceException("Los datos de la cita no pueden ser nulos");
        }

        if (dto.getIdentificacionPaciente() == null || dto.getIdentificacionPaciente().trim().isEmpty()) {
            throw new ServiceException("La identificación del paciente es obligatoria");
        }

        if (dto.getIdMedico() == null || dto.getIdMedico().trim().isEmpty()) {
            throw new ServiceException("El ID del médico es obligatorio");
        }

        // Validar formato UUID del médico
        try {
            UUID.fromString(dto.getIdMedico());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("El ID del médico tiene formato inválido");
        }

        if (dto.getFecha() == null || dto.getFecha().trim().isEmpty()) {
            throw new ServiceException("La fecha es obligatoria");
        }

        if (dto.getHoraInicio() == null || dto.getHoraInicio().trim().isEmpty()) {
            throw new ServiceException("La hora de inicio es obligatoria");
        }

        if (dto.getMotivo() == null || dto.getMotivo().trim().isEmpty()) {
            throw new ServiceException("El motivo de la consulta es obligatorio");
        }

        if (dto.getMotivo().trim().length() < 3) {
            throw new ServiceException("El motivo debe tener al menos 3 caracteres");
        }
    }
}