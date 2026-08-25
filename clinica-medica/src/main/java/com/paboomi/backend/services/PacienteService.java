package com.paboomi.backend.services;

import com.paboomi.backend.dao.PacienteDAO;
import com.paboomi.backend.dto.CitaDTO;
import com.paboomi.backend.dto.PacienteDTO;
import com.paboomi.backend.dto.RegistrarPacienteDTO;
import com.paboomi.backend.models.LogEntry;
import com.paboomi.backend.models.Paciente;
import com.paboomi.backend.util.exceptions.ServiceException;
import lombok.Getter;
import lombok.Setter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class PacienteService {

    private final PacienteDAO pacienteDAO;
    private final CitaService citaService;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private LogService logService;

    public PacienteService(CitaService citaService, LogService logService) throws Exception {
        this.pacienteDAO = new PacienteDAO();
        this.citaService = citaService;
        this.logService = logService;
    }

    //* MÉTODOS DE NEGOCIO

     //* Registra un nuevo paciente en el sistema
    public void registrarPaciente(RegistrarPacienteDTO dto) throws ServiceException {
        try {
            //! Validaciones básicas
            validarDatosPaciente(dto);

            //! Validar que la identificación sea única
            if (pacienteDAO.existePaciente(dto.getIdentificacion())) {
                throw new ServiceException("Ya existe un paciente con la identificación: " + dto.getIdentificacion());
            }

            //! Validar fecha de nacimiento
            Date fechaNacimiento = parseFecha(dto.getFechaNacimiento());
            validarFechaNacimiento(fechaNacimiento);

            //! Validar sexo
            char sexo = dto.getSexo().toUpperCase().charAt(0);
            if (sexo != 'M' && sexo != 'F') {
                throw new ServiceException("Sexo inválido. Debe ser 'M' o 'F'");
            }

            //! Crear la entidad
            Paciente paciente = new Paciente(
                    dto.getIdentificacion(),
                    dto.getNombres(),
                    dto.getApellidos(),
                    fechaNacimiento,
                    sexo,
                    dto.getTelefono(),
                    dto.getEmail(),
                    dto.getTipoSangre()
            );

            //! Guardar
            pacienteDAO.registrarPaciente(paciente);

            //* Registrar en log
            logService.registrarLog(
                    LogEntry.Modulo.PACIENTES,
                    LogEntry.Accion.CREACIÓN,
                    "Paciente registrado: " + paciente.getNombres() + " " + paciente.getApellidos() +
                            " | Identificación: " + paciente.getIdentificacion() +
                            " | Tipo sangre: " + paciente.getTipoSangre(),
                    paciente.getIdentificacion()
            );

        } catch (ParseException e) {
            throw new ServiceException("Formato de fecha inválido. Use yyyy-MM-dd");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al registrar paciente: " + e.getMessage(), e);
        }
    }

    /**
     * Busca un paciente por su identificación
     */
    public PacienteDTO buscarPorIdentificacion(String identificacion) throws ServiceException {
        try {
            if (identificacion == null || identificacion.trim().isEmpty()) {
                throw new ServiceException("La identificación es obligatoria");
            }

            Paciente paciente = pacienteDAO.buscarPorIdentificacion(identificacion);
            if (paciente == null) {
                return null;
            }
            return convertirADTO(paciente);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al buscar paciente: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todos los pacientes
     */
    public List<PacienteDTO> listarTodos() throws ServiceException {
        try {
            List<Paciente> pacientes = pacienteDAO.listarTodos();
            return pacientes.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al listar pacientes: " + e.getMessage(), e);
        }
    }

    /**
     * Busca pacientes por nombre
     */
    public List<PacienteDTO> buscarPorNombre(String nombre) throws ServiceException {
        try {
            if (nombre == null || nombre.trim().isEmpty()) {
                return listarTodos();
            }

            List<Paciente> pacientes = pacienteDAO.buscarPorNombre(nombre.trim());
            return pacientes.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al buscar pacientes por nombre: " + e.getMessage(), e);
        }
    }

    /**
     * Busca pacientes por tipo de sangre
     */
    public List<PacienteDTO> buscarPorTipoSangre(String tipoSangre) throws ServiceException {
        try {
            if (tipoSangre == null || tipoSangre.trim().isEmpty()) {
                throw new ServiceException("El tipo de sangre es obligatorio");
            }

            //* Validar formato de tipo de sangre
            if (!tipoSangre.matches("^[ABO][+-]$")) {
                throw new ServiceException("Tipo de sangre inválido. Formatos válidos: A+, A-, B+, B-, AB+, AB-, O+, O-");
            }

            List<Paciente> pacientes = pacienteDAO.buscarPorTipoSangre(tipoSangre);
            return pacientes.stream()
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al buscar pacientes por tipo de sangre: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza la información de un paciente existente
     */
    public void actualizarPaciente(String identificacion, RegistrarPacienteDTO dto) throws ServiceException {
        try {
            //! Verificar que existe
            Paciente pacienteExistente = pacienteDAO.buscarPorIdentificacion(identificacion);
            if (pacienteExistente == null) {
                throw new ServiceException("Paciente no encontrado con identificación: " + identificacion);
            }

            //! Validaciones
            validarDatosPaciente(dto);

            //! Validar fecha de nacimiento
            Date fechaNacimiento = parseFecha(dto.getFechaNacimiento());
            validarFechaNacimiento(fechaNacimiento);

            //! Validar sexo
            char sexo = dto.getSexo().toUpperCase().charAt(0);
            if (sexo != 'M' && sexo != 'F') {
                throw new ServiceException("Sexo inválido. Debe ser 'M' o 'F'");
            }

            //! Actualizar datos
            pacienteExistente.setNombres(dto.getNombres());
            pacienteExistente.setApellidos(dto.getApellidos());
            pacienteExistente.setFechaNacimiento(fechaNacimiento);
            pacienteExistente.setSexo(sexo);
            pacienteExistente.setNumeroTelefono(dto.getTelefono());
            pacienteExistente.setEmail(dto.getEmail());
            pacienteExistente.setTipoSangre(dto.getTipoSangre());

            //! Guardar cambios
            pacienteDAO.actualizarPaciente(pacienteExistente);

            logService.registrarLog(
                    LogEntry.Modulo.PACIENTES,
                    LogEntry.Accion.ACTUALIZACIÓN,
                    "Paciente actualizado: " + pacienteExistente.getNombres() + " " + pacienteExistente.getApellidos() +
                            " | Identificación: " + pacienteExistente.getIdentificacion(),
                    pacienteExistente.getIdentificacion()
            );

        } catch (ParseException e) {
            throw new ServiceException("Formato de fecha inválido. Use yyyy-MM-dd");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al actualizar paciente: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un paciente
     */
    public void eliminarPaciente(String identificacion) throws ServiceException {
        try {
            if (identificacion == null || identificacion.trim().isEmpty()) {
                throw new ServiceException("La identificación es obligatoria");
            }

            Paciente paciente = pacienteDAO.buscarPorIdentificacion(identificacion);
            if (paciente == null) {
                throw new ServiceException("Paciente no encontrado con identificación: " + identificacion);
            }

            List<CitaDTO> citas = citaService.buscarPorPaciente(paciente.getIdentificacion());
            for (CitaDTO cita : citas) {
                if (cita.getEstado().equals("Programada")) {
                    throw new ServiceException("El paciente: " + paciente.getNombres() + " " + paciente.getApellidos() +
                            " tiene cita programada para: " + cita.getFecha());
                }
            }
            pacienteDAO.eliminarPaciente(identificacion);

            logService.registrarLog(
                    LogEntry.Modulo.PACIENTES,
                    LogEntry.Accion.ELIMINACIÓN,
                    "Paciente eliminado: " + paciente.getNombres() + " " + paciente.getApellidos() +
                            " | Identificación: " + paciente.getIdentificacion(),
                    paciente.getIdentificacion()
            );
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error al eliminar paciente: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si un paciente existe
     */
    public boolean existePaciente(String identificacion) throws ServiceException {
        try {
            if (identificacion == null || identificacion.trim().isEmpty()) {
                return false;
            }
            return pacienteDAO.existePaciente(identificacion);
        } catch (Exception e) {
            throw new ServiceException("Error al verificar existencia del paciente: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene todos los tipos de sangre disponibles
     */
    public List<String> obtenerTiposSangre() throws ServiceException {
        try {
            return pacienteDAO.obtenerTiposSangre().stream().sorted().collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServiceException("Error al obtener tipos de sangre: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene pacientes con mayor cantidad de citas
     */
    public List<PacienteDTO> obtenerPacientesConMasCitas() throws ServiceException {
        try {

            //* Obtener los pacientes
            List<Paciente> pacientes = pacienteDAO.listarTodos();

            //* Almacenar citas por paciente
            Map<String, Integer> conteoCitasPorPaciente = new HashMap<>();
            for (Paciente paciente : pacientes) {
                List<CitaDTO> citas = citaService.buscarPorPaciente(paciente.getIdentificacion());
                int totalCitas = citas.size();

                if (totalCitas > 0) conteoCitasPorPaciente.put(paciente.getIdentificacion(), totalCitas);
            }

            //* Si no hay pacientes con citas, la lista esta vacíá
            if (conteoCitasPorPaciente.isEmpty()) return new ArrayList<>();

            //* Hallar el maximo de citas

            int maxCitas = Collections.max(conteoCitasPorPaciente.values());

            //* Filtrar por maximo de citass
            List<PacienteDTO> pacientesConMasCitas = new ArrayList<>();
            for (Paciente paciente : pacientes) {
                Integer totalCitas = conteoCitasPorPaciente.get(paciente.getIdentificacion());
                if (totalCitas > maxCitas) {
                    PacienteDTO pacienteDTO = convertirADTO(paciente);
                    pacientesConMasCitas.add(pacienteDTO);
                }
            }
            return pacientesConMasCitas;
        } catch (Exception e) {
            throw new ServiceException("Error al obtener pacientes con más citas: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene pacientes que nunca han tenido una cita
     */
    public List<PacienteDTO> obtenerPacientesSinCitas() throws ServiceException {
        try {
            List<Paciente> pacientes = pacienteDAO.listarTodos();

            List<PacienteDTO> pacientesSinCitas = new ArrayList<>();

            for (Paciente paciente : pacientes) {
                List<CitaDTO> citas = citaService.buscarPorPaciente(paciente.getIdentificacion());
                if (!citas.isEmpty()) pacientesSinCitas.add(convertirADTO(paciente));
            }
            return pacientesSinCitas;
        } catch (Exception e) {
            throw new ServiceException("Error al obtener pacientes sin citas: " + e.getMessage(), e);
        }
    }

    //* MÉTODOS PRIVADOS

    /**
     * Convierte un Paciente a PacienteDTO
     */
    private PacienteDTO convertirADTO(Paciente paciente) {
        PacienteDTO dto = new PacienteDTO();
        dto.setIdentificacion(paciente.getIdentificacion());
        dto.setNombresCompletos(paciente.getNombres() + " " + paciente.getApellidos());
        dto.setNombres(paciente.getNombres());
        dto.setApellidos(paciente.getApellidos());
        dto.setFechaNacimiento(DATE_FORMAT.format(paciente.getFechaNacimiento()));
        dto.setSexo(String.valueOf(paciente.getSexo()));
        dto.setTelefono(paciente.getNumeroTelefono());
        dto.setEmail(paciente.getEmail());
        dto.setTipoSangre(paciente.getTipoSangre());
        return dto;
    }

    /**
     * Valida los datos de un paciente
     */
    private void validarDatosPaciente(RegistrarPacienteDTO dto) throws ServiceException {
        if (dto == null) {
            throw new ServiceException("Los datos del paciente no pueden ser nulos");
        }

        if (dto.getIdentificacion() == null || dto.getIdentificacion().trim().isEmpty()) {
            throw new ServiceException("La identificación es obligatoria");
        }

        if (dto.getIdentificacion().trim().length() < 4) {
            throw new ServiceException("La identificación debe tener al menos 4 caracteres");
        }

        if (dto.getNombres() == null || dto.getNombres().trim().isEmpty()) {
            throw new ServiceException("Los nombres son obligatorios");
        }

        if (dto.getApellidos() == null || dto.getApellidos().trim().isEmpty()) {
            throw new ServiceException("Los apellidos son obligatorios");
        }

        if (dto.getFechaNacimiento() == null || dto.getFechaNacimiento().trim().isEmpty()) {
            throw new ServiceException("La fecha de nacimiento es obligatoria");
        }

        if (dto.getSexo() == null || dto.getSexo().trim().isEmpty()) {
            throw new ServiceException("El sexo es obligatorio");
        }

        if (dto.getTelefono() == null || dto.getTelefono().trim().isEmpty()) {
            throw new ServiceException("El teléfono es obligatorio");
        }

        // Validar formato de teléfono
        if (!dto.getTelefono().matches("^[0-9\\-\\+\\s]{8,15}$")) {
            throw new ServiceException("Formato de teléfono inválido");
        }

        // Validar tipo de sangre - Soporta A+, A-, B+, B-, AB+, AB-, O+, O-
        if (!dto.getTipoSangre().matches("^(A|B|AB|O)[+-]$")) {
            throw new ServiceException("Tipo de sangre inválido. Formatos válidos: A+, A-, B+, B-, AB+, AB-, O+, O-");
        }

        if (dto.getTipoSangre() == null || dto.getTipoSangre().trim().isEmpty()) {
            throw new ServiceException("El tipo de sangre es obligatorio");
        }

        // Validar tipo de sangre
        if (!dto.getTipoSangre().matches("^[ABO][+-]$")) {
            throw new ServiceException("Tipo de sangre inválido. Formatos válidos: A+, A-, B+, B-, AB+, AB-, O+, O-");
        }
    }

    /**
     * Valida que la fecha de nacimiento tenga sentido XD
     */
    private void validarFechaNacimiento(Date fechaNacimiento) throws ServiceException {
        if (fechaNacimiento == null) {
            throw new ServiceException("La fecha de nacimiento es obligatoria");
        }

        Date hoy = new Date();
        if (fechaNacimiento.after(hoy)) {
            throw new ServiceException("La fecha de nacimiento no puede ser futura"); //* por si acaso
        }

        //* Calcular edad aproximada (en años)
        long edadEnMillis = hoy.getTime() - fechaNacimiento.getTime();
        long edadEnAnios = edadEnMillis / (1000L * 60 * 60 * 24 * 365);

        if (edadEnAnios < 0) {
            throw new ServiceException("La fecha de nacimiento no puede ser futura"); //* Lo mismo que arriba pero mejor
        }
    }

    /**
     * Parsea una fecha en formato yyyy-MM-dd
     */
    private Date parseFecha(String fechaStr) throws ParseException {
        if (fechaStr == null || fechaStr.trim().isEmpty()) {
            throw new ParseException("Fecha vacía", 0);
        }
        return DATE_FORMAT.parse(fechaStr.trim());
    }
}