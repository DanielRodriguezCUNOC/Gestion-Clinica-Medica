package com.paboomi.backend.dao;

import com.paboomi.backend.models.Medico;
import com.paboomi.backend.util.files.FileUtil;
import com.paboomi.backend.util.ring.IndexHandler;
import com.paboomi.backend.util.ring.MedicoSerializador;
import com.paboomi.backend.util.ring.RingFileHandler;
import com.paboomi.backend.util.ring.SecondaryIndexHandler;
import lombok.Getter;
import lombok.Setter;

import java.io.RandomAccessFile;
import java.util.*;

@Getter
@Setter
public class MedicoDAO {

    //* RUTAS DE ARCHIVOS
    private static final String RUTA_DATOS = "data/medicos/medicos.dat";
    private static final String RUTA_IDX_UUID = "data/medicos/medicos_idx_uuid.dat";
    private static final String RUTA_IDX_ESPECIALIDAD = "data/medicos/medicos_idx_especialidad.dat";

    //* CONSTANTES
    private static final int LONGITUD_UUID = 36;
    private static final int LONGITUD_ESPECIALIDAD = 40;

    //* COMPONENTES
    private final RingFileHandler ringHandler;
    private final IndexHandler indexUuidHandler;
    private final SecondaryIndexHandler indexEspecialidadHandler;
    private final MedicoSerializador serializador;

    //* CACHÉ
    private final Map<String, Long> cacheUuidPosicion;
    private final Map<String, List<Long>> cacheEspecialidadPosiciones;

    //* CONSTRUCTOR
    public MedicoDAO() throws Exception {

        //* Crear directorios
        FileUtil.crearDirectoriosParaArchivos(
                RUTA_DATOS,
                RUTA_IDX_UUID,
                RUTA_IDX_ESPECIALIDAD
        );

        //* Inicializar serializador
        this.serializador = new MedicoSerializador();

        //* Inicializar handlers
        int tamanioAnillo = 1 + serializador.getTamanioRegistro(); // bitmap + datos
        this.ringHandler = new RingFileHandler(RUTA_DATOS, tamanioAnillo);
        this.indexUuidHandler = new IndexHandler(RUTA_IDX_UUID, LONGITUD_UUID);
        this.indexEspecialidadHandler = new SecondaryIndexHandler(
                RUTA_IDX_ESPECIALIDAD,
                LONGITUD_ESPECIALIDAD,
                RUTA_DATOS
        );

        //* Inicializar caché
        this.cacheUuidPosicion = new HashMap<>();
        this.cacheEspecialidadPosiciones = new HashMap<>();

        //* Cargar caché
        cargarCache();
    }

    //* INICIALIZACIÓN
    private void cargarCache() throws Exception {
        //* Cargar índice UUID
        cacheUuidPosicion.clear();
        cacheUuidPosicion.putAll(indexUuidHandler.cargarTodos());

        //* Cargar índice de especialidad
        cacheEspecialidadPosiciones.clear();
        cacheEspecialidadPosiciones.putAll(indexEspecialidadHandler.cargarTodos());

        //* Recargar espacios libres
        ringHandler.recargarEspaciosLibres();
    }

    //* MÉTODOS CRUD

    public void registrarMedico(Medico medico) throws Exception {
        //* Obtener espacio libre
        int posicionRegistro = ringHandler.obtenerEspacioLibre();
        long posicionFisica = ringHandler.getPosicionFisica(posicionRegistro);

        // scribir en el archivo de anillo
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "rw")) {
            raf.seek(posicionFisica);
            ringHandler.marcarEstado(raf, true);
            serializador.escribir(raf, medico);
        }

        //* Actualizar índices
        indexUuidHandler.agregar(medico.getId().toString(), posicionFisica);
        indexEspecialidadHandler.agregar(medico.getEspecialidad(), posicionFisica);

        //* Actualizar caché
        cacheUuidPosicion.put(medico.getId().toString(), posicionFisica);
        cacheEspecialidadPosiciones.computeIfAbsent(
                medico.getEspecialidad(),
                k -> new ArrayList<>()
        ).add(posicionFisica);
    }

    public Medico buscarPorId(UUID id) throws Exception {
        String uuidStr = id.toString();

        //* Buscar en caché
        Long posicionFisica = cacheUuidPosicion.get(uuidStr);
        if (posicionFisica == null) {
            //* Intentar en el índice (por si la caché no está actualizada)
            posicionFisica = indexUuidHandler.buscar(uuidStr);
            if (posicionFisica == null) {
                return null;
            }
        }

        //* Validar que el registro esté ocupado
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            raf.seek(posicionFisica);
            if (!ringHandler.estaOcupado(raf)) {
                //* Inconsistencia enconces limpiar caché
                cacheUuidPosicion.remove(uuidStr);
                return null;
            }
            return serializador.leer(raf);
        }
    }

    public List<Medico> listarTodos() throws Exception {
        List<Medico> medicos = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            long cantidadRegistros = ringHandler.getCantidadRegistros();

            for (int i = 0; i < cantidadRegistros; i++) {
                raf.seek(ringHandler.getPosicionFisica(i));
                if (ringHandler.estaOcupado(raf)) {
                    medicos.add(serializador.leer(raf));
                }
            }
        }
        return medicos;
    }

    public List<Medico> buscarPorNombre(String nombre) throws Exception {
        List<Medico> resultado = new ArrayList<>();
        String busqueda = nombre.toLowerCase();

        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            long cantidadRegistros = ringHandler.getCantidadRegistros();

            for (int i = 0; i < cantidadRegistros; i++) {
                raf.seek(ringHandler.getPosicionFisica(i));
                if (ringHandler.estaOcupado(raf)) {
                    Medico medico = serializador.leer(raf);
                    if (medico.getNombres().toLowerCase().contains(busqueda) ||
                            medico.getApellidos().toLowerCase().contains(busqueda)) {
                        resultado.add(medico);
                    }
                }
            }
        }
        return resultado;
    }

    public List<Medico> buscarPorEspecialidad(String especialidad) throws Exception {
        //* Buscar en caché o índice
        List<Long> posiciones = cacheEspecialidadPosiciones.get(especialidad);
        if (posiciones == null) {
            posiciones = indexEspecialidadHandler.buscar(especialidad);
            if (posiciones == null || posiciones.isEmpty()) {
                return new ArrayList<>();
            }
        }

        //* Leer los médicos desde las posiciones
        List<Medico> resultado = new ArrayList<>();
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

    public List<Medico> listarActivos() throws Exception {
        List<Medico> activos = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            long cantidadRegistros = ringHandler.getCantidadRegistros();

            for (int i = 0; i < cantidadRegistros; i++) {
                raf.seek(ringHandler.getPosicionFisica(i));
                if (ringHandler.estaOcupado(raf)) {
                    Medico medico = serializador.leer(raf);
                    if (medico.isActivo()) {
                        activos.add(medico);
                    }
                }
            }
        }
        return activos;
    }

    public List<Medico> listarInactivos() throws Exception {
        List<Medico> inactivos = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            long cantidadRegistros = ringHandler.getCantidadRegistros();

            for (int i = 0; i < cantidadRegistros; i++) {
                raf.seek(ringHandler.getPosicionFisica(i));
                if (ringHandler.estaOcupado(raf)) {
                    Medico medico = serializador.leer(raf);
                    if (!medico.isActivo()) {
                        inactivos.add(medico);
                    }
                }
            }
        }
        return inactivos;
    }

    public void actualizarMedico(Medico medico) throws Exception {
        String uuidStr = medico.getId().toString();
        Long posicionFisica = cacheUuidPosicion.get(uuidStr);

        if (posicionFisica == null) {
            throw new Exception("Médico no encontrado con ID: " + uuidStr);
        }

        //* Obtener el médico viejo para comparar
        Medico medicoViejo = buscarPorId(medico.getId());
        if (medicoViejo == null) {
            throw new Exception("Médico no encontrado con ID: " + uuidStr);
        }

        //* Actualizar en el archivo de anillo
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "rw")) {
            raf.seek(posicionFisica);
            ringHandler.marcarEstado(raf, true);
            serializador.escribir(raf, medico);
        }

        //* Actualizar índice de especialidad si cambió
        String especialidadVieja = medicoViejo.getEspecialidad();
        String especialidadNueva = medico.getEspecialidad();

        if (!especialidadVieja.equals(especialidadNueva)) {
            //* Eliminar de la especialidad vieja
            List<Long> posViejas = cacheEspecialidadPosiciones.get(especialidadVieja);
            if (posViejas != null) {
                posViejas.remove(posicionFisica);
                indexEspecialidadHandler.eliminar(especialidadVieja, posicionFisica);
            }

            //* Agregar a la especialidad nueva
            cacheEspecialidadPosiciones.computeIfAbsent(
                    especialidadNueva,
                    k -> new ArrayList<>()
            ).add(posicionFisica);
            indexEspecialidadHandler.agregar(especialidadNueva, posicionFisica);
        }

        //* Actualizar caché UUID (la posición no cambia)
        cacheUuidPosicion.put(uuidStr, posicionFisica);
    }

    public void desactivarMedico(UUID id) throws Exception {
        Medico medico = buscarPorId(id);
        if (medico == null) {
            throw new Exception("Médico no encontrado con ID: " + id);
        }
        medico.setActivo(false);
        actualizarMedico(medico);
    }

    public void activarMedico(UUID id) throws Exception {
        Medico medico = buscarPorId(id);
        if (medico == null) {
            throw new Exception("Médico no encontrado con ID: " + id);
        }
        medico.setActivo(true);
        actualizarMedico(medico);
    }

    public void eliminarMedico(UUID id) throws Exception {
        String uuidStr = id.toString();
        Long posicionFisica = cacheUuidPosicion.get(uuidStr);

        if (posicionFisica == null) {
            throw new Exception("Médico no encontrado con ID: " + uuidStr);
        }

        //* Obtener especialidad antes de eliminar
        Medico medico = buscarPorId(id);
        if (medico == null) {
            throw new Exception("Médico no encontrado con ID: " + uuidStr);
        }

        //* Marcar como libre en el archivo de anillo
        int posicionRegistro = (int) (posicionFisica / (1 + serializador.getTamanioRegistro()));
        ringHandler.liberarEspacio(posicionRegistro);

        //* Limpiar caché
        cacheUuidPosicion.remove(uuidStr);

    }

    public boolean existeMedico(UUID id) throws Exception {
        return buscarPorId(id) != null;
    }

    //* MÉTODOS DE APOYO

    /**
     * Obtiene todos los médicos activos
     */
    public List<Medico> obtenerMedicosActivos() throws Exception {
        return listarActivos();
    }

    /**
     * Obtiene todas las especialidades disponibles
     */
    public Set<String> obtenerEspecialidades() throws Exception {
        return new HashSet<>(cacheEspecialidadPosiciones.keySet());
    }

    /**
     * Cuenta cuántos médicos activos hay por especialidad
     */
    public Map<String, Long> contarMedicosPorEspecialidad() throws Exception {
        Map<String, Long> conteo = new HashMap<>();
        List<Medico> activos = listarActivos();

        for (Medico medico : activos) {
            String esp = medico.getEspecialidad();
            conteo.put(esp, conteo.getOrDefault(esp, 0L) + 1);
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