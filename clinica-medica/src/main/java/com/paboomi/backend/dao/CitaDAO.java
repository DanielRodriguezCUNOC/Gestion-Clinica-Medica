package com.paboomi.backend.dao;

import com.paboomi.backend.models.Cita;
import com.paboomi.backend.util.ring.CitaSerializador;
import com.paboomi.backend.util.ring.IndexHandler;
import com.paboomi.backend.util.ring.RingFileHandler;
import com.paboomi.backend.util.ring.SecondaryIndexHandler;
import lombok.Getter;
import lombok.Setter;

import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class CitaDAO {

    //* RUTAS DE ARCHIVOS
    private static final String RUTA_DATOS = "citas.dat";
    private static final String RUTA_IDX_UUID = "citas_idx_uuid.dat";
    private static final String RUTA_IDX_PACIENTE = "citas_idx_paciente.dat";
    private static final String RUTA_IDX_MEDICO = "citas_idx_medico.dat";
    private static final String RUTA_IDX_FECHA = "citas_idx_fecha.dat";
    private static final String RUTA_IDX_ESTADO = "citas_idx_estado.dat";

    //* CONSTANTES
    private static final int LONGITUD_UUID = 36;
    private static final int LONGITUD_ID_PACIENTE = 20;
    private static final int LONGITUD_UUID_MEDICO = 36;
    private static final int LONGITUD_FECHA = 10;  // yyyy-MM-dd
    private static final int LONGITUD_ESTADO = 15;

    // * COMPONENTES
    private final RingFileHandler ringHandler;
    private final IndexHandler indexUuidHandler;
    private final SecondaryIndexHandler indexPacienteHandler;
    private final SecondaryIndexHandler indexMedicoHandler;
    private final SecondaryIndexHandler indexFechaHandler;
    private final SecondaryIndexHandler indexEstadoHandler;
    private final CitaSerializador serializador;

    //* CACHÉ
    private final Map<String, Long> cacheUuidPosicion;
    private final Map<String, List<Long>> cachePacientePosiciones;
    private final Map<String, List<Long>> cacheMedicoPosiciones;
    private final Map<String, List<Long>> cacheFechaPosiciones;
    private final Map<String, List<Long>> cacheEstadoPosiciones;


    public CitaDAO() throws Exception {
        //* Inicializar serializador
        this.serializador = new CitaSerializador();

        //* Inicializar handlers
        int tamanioAnillo = 1 + serializador.getTamanioRegistro(); // bitmap + datos
        this.ringHandler = new RingFileHandler(RUTA_DATOS, tamanioAnillo);
        this.indexUuidHandler = new IndexHandler(RUTA_IDX_UUID, tamanioAnillo);
        this.indexPacienteHandler = new SecondaryIndexHandler(
                RUTA_IDX_PACIENTE,
                LONGITUD_ID_PACIENTE,
                RUTA_DATOS
        );
        this.indexMedicoHandler = new SecondaryIndexHandler(
                RUTA_IDX_MEDICO,
                LONGITUD_UUID_MEDICO,
                RUTA_DATOS
        );
        this.indexFechaHandler = new SecondaryIndexHandler(
                RUTA_IDX_FECHA,
                LONGITUD_FECHA,
                RUTA_DATOS
        );
        this.indexEstadoHandler = new SecondaryIndexHandler(
                RUTA_IDX_ESTADO,
                LONGITUD_ESTADO,
                RUTA_DATOS
        );

        //* Inicializar caché
        this.cacheUuidPosicion = new HashMap<>();
        this.cachePacientePosiciones = new HashMap<>();
        this.cacheMedicoPosiciones = new HashMap<>();
        this.cacheFechaPosiciones = new HashMap<>();
        this.cacheEstadoPosiciones = new HashMap<>();

        //* Cargar caché
        cargarCache();
    }

    //* INICIALIZACIÓN
    private void cargarCache() throws Exception {
        //* Cargar todos los índices
        cacheUuidPosicion.clear();
        cachePacientePosiciones.clear();
        cachePacientePosiciones.putAll(indexPacienteHandler.cargarTodos());

        cacheMedicoPosiciones.clear();
        cacheMedicoPosiciones.putAll(indexMedicoHandler.cargarTodos());

        cacheFechaPosiciones.clear();
        cacheFechaPosiciones.putAll(indexFechaHandler.cargarTodos());

        cacheEstadoPosiciones.clear();
        cacheEstadoPosiciones.putAll(indexEstadoHandler.cargarTodos());

        //* Recargar espacios libres
        ringHandler.recargarEspaciosLibres();
    }

    //* MÉTODOS CRUD

    public void registrarCita(Cita cita) throws Exception {

        //* Obtener espacio libre
        int posicionRegistro = ringHandler.obtenerEspacioLibre();
        long posicionFisica = ringHandler.getPosicionFisica(posicionRegistro);

        //* Escribir en el archivo de anillo
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "rw")) {
            raf.seek(posicionFisica);
            ringHandler.marcarEstado(raf, true);
            serializador.escribir(raf, cita);
        }

        //* Actualizar índices
        String uuidStr = cita.getId().toString();
        String idPaciente = cita.getIdPaciente();
        String idMedico = cita.getIdMedico().toString();
        String fechaStr = cita.getFechaCita().toString();
        String estado = cita.getEstado();

        indexUuidHandler.agregar(uuidStr, posicionFisica);
        indexPacienteHandler.agregar(idPaciente, posicionFisica);
        indexMedicoHandler.agregar(idMedico, posicionFisica);
        indexFechaHandler.agregar(fechaStr, posicionFisica);
        indexEstadoHandler.agregar(estado, posicionFisica);

        //* Actualizar caché
        cacheUuidPosicion.put(uuidStr, posicionFisica);
        cachePacientePosiciones.computeIfAbsent(idPaciente, k -> new ArrayList<>()).add(posicionFisica);
        cacheMedicoPosiciones.computeIfAbsent(idMedico, k -> new ArrayList<>()).add(posicionFisica);
        cacheFechaPosiciones.computeIfAbsent(fechaStr, k -> new ArrayList<>()).add(posicionFisica);
        cacheEstadoPosiciones.computeIfAbsent(estado, k -> new ArrayList<>()).add(posicionFisica);
    }

    public Cita buscarPorId(UUID id) throws Exception {
        String uuidStr = id.toString();

        //* Bucar en Cache
        Long posicionFisica = cacheUuidPosicion.get(uuidStr);
        if (posicionFisica == null) {
            posicionFisica = indexUuidHandler.buscar(uuidStr);
            if(posicionFisica == null) return null;
        }

        //* Validar que el registro esté ocupado
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
                raf.seek(posicionFisica);
                if (!ringHandler.estaOcupado(raf)) {
                    cacheUuidPosicion.remove(uuidStr);
                }

            return serializador.leer(raf);
        }
    }

    public List<Cita> listarTodos() throws Exception {
        List<Cita> citas = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            long cantidadRegistros = ringHandler.getCantidadRegistros();

            for (int i = 0; i < cantidadRegistros; i++) {
                raf.seek(ringHandler.getPosicionFisica(i));
                if (ringHandler.estaOcupado(raf)) {
                    citas.add(serializador.leer(raf));
                }
            }
        }
        return citas;
    }

    public List<Cita> buscarPorPaciente(String identificacionPaciente) throws Exception {
        //* Buscar en caché o índice
        List<Long> posiciones = cachePacientePosiciones.get(identificacionPaciente);
        if (posiciones == null) {
            posiciones = indexPacienteHandler.buscar(identificacionPaciente);
            if (posiciones == null || posiciones.isEmpty()) {
                return new ArrayList<>();
            }
        }

        //* Leer las citas desde las posiciones
        List<Cita> resultado = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            for (Long posicion : posiciones) {
                raf.seek(posicion);
                if (ringHandler.estaOcupado(raf)) {
                    resultado.add(serializador.leer(raf));
                }
            }
        }
        return resultado;
    }

    public List<Cita> buscarPorMedico(UUID idMedico) throws Exception {
        String idMedicoStr = idMedico.toString();

        //* Buscar en caché o índice
        List<Long> posiciones = cacheMedicoPosiciones.get(idMedicoStr);
        if (posiciones == null) {
            posiciones = indexMedicoHandler.buscar(idMedicoStr);
            if (posiciones == null || posiciones.isEmpty()) {
                return new ArrayList<>();
            }
        }

        //* Leer las citas desde las posiciones
        List<Cita> resultado = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            for (Long posicion : posiciones) {
                raf.seek(posicion);
                if (ringHandler.estaOcupado(raf)) {
                    resultado.add(serializador.leer(raf));
                }
            }
        }
        return resultado;
    }

    public List<Cita> buscarPorFecha(LocalDate fecha) throws Exception {
        String fechaStr = fecha.toString();

        //* Buscar en caché o índice
        List<Long> posiciones = cacheFechaPosiciones.get(fechaStr);
        if (posiciones == null) {
            posiciones = indexFechaHandler.buscar(fechaStr);
            if (posiciones == null || posiciones.isEmpty()) {
                return new ArrayList<>();
            }
        }

        //* Leer las citas desde las posiciones
        List<Cita> resultado = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            for (Long posicion : posiciones) {
                raf.seek(posicion);
                if (ringHandler.estaOcupado(raf)) {
                    resultado.add(serializador.leer(raf));
                }
            }
        }
        return resultado;
    }

    public List<Cita> buscarPorEstado(String estado) throws Exception {
        //* Buscar en caché o índice
        List<Long> posiciones = cacheEstadoPosiciones.get(estado);
        if (posiciones == null) {
            posiciones = indexEstadoHandler.buscar(estado);
            if (posiciones == null || posiciones.isEmpty()) {
                return new ArrayList<>();
            }
        }

        //* Leer las citas desde las posiciones
        List<Cita> resultado = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            for (Long posicion : posiciones) {
                raf.seek(posicion);
                if (ringHandler.estaOcupado(raf)) {
                    resultado.add(serializador.leer(raf));
                }
            }
        }
        return resultado;
    }

    public List<Cita> buscarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) throws Exception {
        List<Cita> resultado = new ArrayList<>();

        //! Búsqueda secuencial (Se podría mejorar a O(k*m) pero se debe implementar un B-Tree)
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            long cantidadRegistros = ringHandler.getCantidadRegistros();

            for (int i = 0; i < cantidadRegistros; i++) {
                raf.seek(ringHandler.getPosicionFisica(i));
                if (ringHandler.estaOcupado(raf)) {
                    Cita cita = serializador.leer(raf);
                    LocalDate fechaCita = cita.getFechaCita();
                    if (!fechaCita.isBefore(fechaInicio) && !fechaCita.isAfter(fechaFin)) {
                        resultado.add(cita);
                    }
                }
            }
        }
        return resultado;
    }

    public void actualizarCita(Cita cita) throws Exception {

        String uuidStr = cita.getId().toString();

        Long posicionFisica = cacheUuidPosicion.get(uuidStr);
        if (posicionFisica == null) {
            posicionFisica = indexUuidHandler.buscar(uuidStr);
            if (posicionFisica == null) {
                throw new Exception("Cita no encontrada con ID: " + cita.getId());
            }
        }
        //* Validar que el registro esté ocupado
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            long cantidadRegistros = ringHandler.getCantidadRegistros();

            for (int i = 0; i < cantidadRegistros; i++) {
                raf.seek(posicionFisica);
                if (!ringHandler.estaOcupado(raf)) {
                    cacheUuidPosicion.remove(uuidStr);
                    throw new Exception("Cita no encontrada con ID: " + cita.getId());
                }
            }
        }

        Cita citaAnterior = buscarPorId(cita.getId());
        if (citaAnterior == null) {
            throw new Exception("No se ha registrado una cita con ID: " + cita.getId());
        }

        //* Actualizar en el archivo de anillo
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "rw")) {
            raf.seek(posicionFisica);
            ringHandler.marcarEstado(raf, true);
            serializador.escribir(raf, cita);
        }

        //* Actualizar índices si hubo cambios
        actualizarIndices(citaAnterior, cita, posicionFisica);
    }

    private void actualizarIndices(Cita vieja, Cita nueva, long posicionFisica) throws Exception {
        //* Actualizar índice por paciente
        if (!vieja.getIdPaciente().equals(nueva.getIdPaciente())) {
            indexPacienteHandler.eliminar(vieja.getIdPaciente(), posicionFisica);
            indexPacienteHandler.agregar(nueva.getIdPaciente(), posicionFisica);

            cachePacientePosiciones.get(vieja.getIdPaciente()).remove(posicionFisica);
            cachePacientePosiciones.computeIfAbsent(nueva.getIdPaciente(), k -> new ArrayList<>()).add(posicionFisica);
        }

        //* Actualizar índice por médico
        if (!vieja.getIdMedico().equals(nueva.getIdMedico())) {
            String viejoMedico = vieja.getIdMedico().toString();
            String nuevoMedico = nueva.getIdMedico().toString();

            indexMedicoHandler.eliminar(viejoMedico, posicionFisica);
            indexMedicoHandler.agregar(nuevoMedico, posicionFisica);

            cacheMedicoPosiciones.get(viejoMedico).remove(posicionFisica);
            cacheMedicoPosiciones.computeIfAbsent(nuevoMedico, k -> new ArrayList<>()).add(posicionFisica);
        }

        //* Actualizar índice por fecha
        if (!vieja.getFechaCita().equals(nueva.getFechaCita())) {
            String viejaFecha = vieja.getFechaCita().toString();
            String nuevaFecha = nueva.getFechaCita().toString();

            indexFechaHandler.eliminar(viejaFecha, posicionFisica);
            indexFechaHandler.agregar(nuevaFecha, posicionFisica);

            cacheFechaPosiciones.get(viejaFecha).remove(posicionFisica);
            cacheFechaPosiciones.computeIfAbsent(nuevaFecha, k -> new ArrayList<>()).add(posicionFisica);
        }

        //* Actualizar índice por estado
        if (!vieja.getEstado().equals(nueva.getEstado())) {
            indexEstadoHandler.eliminar(vieja.getEstado(), posicionFisica);
            indexEstadoHandler.agregar(nueva.getEstado(), posicionFisica);

            cacheEstadoPosiciones.get(vieja.getEstado()).remove(posicionFisica);
            cacheEstadoPosiciones.computeIfAbsent(nueva.getEstado(), k -> new ArrayList<>()).add(posicionFisica);
        }
    }

    public void cancelarCita(UUID id) throws Exception {
        Cita cita = buscarPorId(id);
        if (cita == null) {
            throw new Exception("Cita no encontrada con ID: " + id);
        }

        if (cita.getEstado().equals("Atendida")) {
            throw new Exception("No se puede cancelar una cita que ya fue atendida");
        }

        cita.setEstado("Cancelada");
        actualizarCita(cita);
    }

    public void marcarComoAtendida(UUID id) throws Exception {
        Cita cita = buscarPorId(id);
        if (cita == null) {
            throw new Exception("Cita no encontrada con ID: " + id);
        }

        if (cita.getEstado().equals("Cancelada")) {
            throw new Exception("No se puede marcar como atendida una cita cancelada");
        }

        cita.setEstado("Atendida");
        actualizarCita(cita);
    }

    public void eliminarCita(UUID id) throws Exception {
        String uuidStr = id.toString();

        Long posicionFisica = cacheUuidPosicion.get(uuidStr);
        if (posicionFisica == null) {
            posicionFisica = indexUuidHandler.buscar(uuidStr);
            if (posicionFisica == null) {
                throw new Exception("Cita no encontrada con ID: " + id);
            }
        }

        //* Verificar que exista
        Cita cita = buscarPorId(id);
        if (cita == null) {
            throw new Exception("Cita no encontrada con ID: " + id);
        }

        //* Marcar como libre en el archivo de anillo (SOFT DELETE)
        int posicionRegistro = (int) (posicionFisica / (1 + serializador.getTamanioRegistro()));
        ringHandler.liberarEspacio(posicionRegistro);

        //* Limpiar caché UUID (opcional)
        cacheUuidPosicion.remove(uuidStr);
    }

    public boolean existeCita(UUID id) throws Exception {
        return buscarPorId(id) != null;
    }

    //* MÉTODOS PARA REPORTES

    /**
     * Obtiene el conteo de citas por estado
     */
    public Map<String, Long> contarCitasPorEstado() throws Exception {
        Map<String, Long> conteo = new HashMap<>();
        List<Cita> citas = listarTodos();

        for (Cita cita : citas) {
            String estado = cita.getEstado();
            conteo.put(estado, conteo.getOrDefault(estado, 0L) + 1);
        }

        return conteo;
    }

    /**
     * Obtiene el conteo de citas por especialidad
     */
    public Map<String, Long> contarCitasPorEspecialidad(Map<UUID, String> medicoEspecialidadMap) throws Exception {
        Map<String, Long> conteo = new HashMap<>();
        List<Cita> citas = listarTodos();

        for (Cita cita : citas) {
            String especialidad = medicoEspecialidadMap.get(cita.getIdMedico());
            if (especialidad != null) {
                conteo.put(especialidad, conteo.getOrDefault(especialidad, 0L) + 1);
            }
        }

        return conteo;
    }

    /**
     * Recarga la caché desde los archivos
     */
    public void recargarCache() throws Exception {
        cargarCache();
    }
}