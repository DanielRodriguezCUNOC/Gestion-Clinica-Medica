package com.paboomi.backend.dao;

import com.paboomi.backend.models.Paciente;
import com.paboomi.backend.util.files.FileUtil;
import com.paboomi.backend.util.ring.IndexHandler;
import com.paboomi.backend.util.ring.PacienteSerializador;
import com.paboomi.backend.util.ring.RingFileHandler;
import com.paboomi.backend.util.ring.SecondaryIndexHandler;
import lombok.Getter;
import lombok.Setter;

import java.io.RandomAccessFile;
import java.util.*;

@Getter
@Setter
public class PacienteDAO {

    //* RUTAS DE ARCHIVOS
    private static final String RUTA_DATOS = "data/pacientes/pacientes.dat";
    private static final String RUTA_IDX_IDENTIFICACION = "data/pacientes/pacientes_idx_identificacion.dat";
    private static final String RUTA_IDX_TIPO_SANGRE = "data/pacientes/pacientes_idx_tipo_sangre.dat";

    //* CONSTANTES
    private static final int LONGITUD_IDENTIFICACION = 20;
    private static final int LONGITUD_TIPO_SANGRE = 3;

    //* COMPONENTES
    private final RingFileHandler ringHandler;
    private final IndexHandler indexIdentificacionHandler;
    private final SecondaryIndexHandler indexTipoSangreHandler;
    private final PacienteSerializador serializador;

    //* CACHÉ
    private final Map<String, Long> cacheIdentificacionPosicion;
    private final Map<String, List<Long>> cacheTipoSangrePosiciones;

    public PacienteDAO() throws Exception {

        //* Crear directorios
        FileUtil.crearDirectoriosParaArchivos(
                RUTA_DATOS,
                RUTA_IDX_IDENTIFICACION,
                RUTA_IDX_TIPO_SANGRE
        );


        //* Inicializar serializador
        this.serializador = new PacienteSerializador();

        //* Inicializar handlers
        int tamanioAnillo = 1 + serializador.getTamanioRegistro(); // bitmap + datos
        this.ringHandler = new RingFileHandler(RUTA_DATOS, tamanioAnillo);
        this.indexIdentificacionHandler = new IndexHandler(RUTA_IDX_IDENTIFICACION, LONGITUD_IDENTIFICACION);
        this.indexTipoSangreHandler = new SecondaryIndexHandler(
                RUTA_IDX_TIPO_SANGRE,
                LONGITUD_TIPO_SANGRE,
                RUTA_DATOS
        );

        // Inicializar caché
        this.cacheIdentificacionPosicion = new HashMap<>();
        this.cacheTipoSangrePosiciones = new HashMap<>();

        //* Cargar caché
        cargarCache();
    }

    // ============ INICIALIZACIÓN ============
    private void cargarCache() throws Exception {
        //* Cargar índice por identificación
        cacheIdentificacionPosicion.clear();
        cacheIdentificacionPosicion.putAll(indexIdentificacionHandler.cargarTodos());

        //* Cargar índice por tipo de sangre
        cacheTipoSangrePosiciones.clear();
        cacheTipoSangrePosiciones.putAll(indexTipoSangreHandler.cargarTodos());

        //* Recargar espacios libres
        ringHandler.recargarEspaciosLibres();
    }

    //* MÉTODOS CRUD

    public void registrarPaciente(Paciente paciente) throws Exception {
        //* Validar que no exista un paciente con la misma identificación
        if (existePaciente(paciente.getIdentificacion())) {
            throw new Exception("Ya existe un paciente con la identificación: " + paciente.getIdentificacion());
        }

        //* Obtener espacio libre
        int posicionRegistro = ringHandler.obtenerEspacioLibre();
        long posicionFisica = ringHandler.getPosicionFisica(posicionRegistro);

        //* Escribir en el archivo de anillo
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "rw")) {
            raf.seek(posicionFisica);
            ringHandler.marcarEstado(raf, true);
            serializador.escribir(raf, paciente);
        }

        //* Actualizar índices
        indexIdentificacionHandler.agregar(paciente.getIdentificacion(), posicionFisica);
        indexTipoSangreHandler.agregar(paciente.getTipoSangre(), posicionFisica);

        //* Actualizar caché
        cacheIdentificacionPosicion.put(paciente.getIdentificacion(), posicionFisica);
        cacheTipoSangrePosiciones.computeIfAbsent(
                paciente.getTipoSangre(),
                k -> new ArrayList<>()
        ).add(posicionFisica);
    }

    public Paciente buscarPorIdentificacion(String identificacion) throws Exception {
        //* Buscar en caché
        Long posicionFisica = cacheIdentificacionPosicion.get(identificacion);
        if (posicionFisica == null) {
            // Intentar en el índice (por si la caché no está actualizada)
            posicionFisica = indexIdentificacionHandler.buscar(identificacion);
            if (posicionFisica == null) {
                return null;
            }
        }

        //* Validar que el registro esté ocupado
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            raf.seek(posicionFisica);
            if (!ringHandler.estaOcupado(raf)) {
                // Inconsistencia: limpiar caché
                cacheIdentificacionPosicion.remove(identificacion);
                return null;
            }
            return serializador.leer(raf);
        }
    }

    public List<Paciente> listarTodos() throws Exception {
        List<Paciente> pacientes = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            long cantidadRegistros = ringHandler.getCantidadRegistros();

            for (int i = 0; i < cantidadRegistros; i++) {
                raf.seek(ringHandler.getPosicionFisica(i));
                if (ringHandler.estaOcupado(raf)) {
                    pacientes.add(serializador.leer(raf));
                }
            }
        }
        return pacientes;
    }

    public List<Paciente> buscarPorNombre(String nombre) throws Exception {
        List<Paciente> resultado = new ArrayList<>();
        String busqueda = nombre.toLowerCase();

        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "r")) {
            long cantidadRegistros = ringHandler.getCantidadRegistros();

            for (int i = 0; i < cantidadRegistros; i++) {
                raf.seek(ringHandler.getPosicionFisica(i));
                if (ringHandler.estaOcupado(raf)) {
                    Paciente paciente = serializador.leer(raf);
                    if (paciente.getNombres().toLowerCase().contains(busqueda) ||
                            paciente.getApellidos().toLowerCase().contains(busqueda)) {
                        resultado.add(paciente);
                    }
                }
            }
        }
        return resultado;
    }

    public List<Paciente> buscarPorTipoSangre(String tipoSangre) throws Exception {
        //* Buscar en caché o índice
        List<Long> posiciones = cacheTipoSangrePosiciones.get(tipoSangre);
        if (posiciones == null) {
            posiciones = indexTipoSangreHandler.buscar(tipoSangre);
            if (posiciones == null || posiciones.isEmpty()) {
                return new ArrayList<>();
            }
        }

        //* Leer los pacientes desde las posiciones
        List<Paciente> resultado = new ArrayList<>();
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

    public void actualizarPaciente(Paciente paciente) throws Exception {
        String identificacion = paciente.getIdentificacion();
        Long posicionFisica = cacheIdentificacionPosicion.get(identificacion);

        if (posicionFisica == null) {
            throw new Exception("Paciente no encontrado con identificación: " + identificacion);
        }

        //* Obtener el paciente viejo para comparar
        Paciente pacienteViejo = buscarPorIdentificacion(identificacion);
        if (pacienteViejo == null) {
            throw new Exception("Paciente no encontrado con identificación: " + identificacion);
        }

        //* Actualizar en el archivo de anillo
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_DATOS, "rw")) {
            raf.seek(posicionFisica);
            ringHandler.marcarEstado(raf, true);
            serializador.escribir(raf, paciente);
        }

        //* Actualizar índice de tipo de sangre si cambió
        String tipoSangreViejo = pacienteViejo.getTipoSangre();
        String tipoSangreNuevo = paciente.getTipoSangre();

        if (!tipoSangreViejo.equals(tipoSangreNuevo)) {
            //* Eliminar del tipo de sangre viejo
            List<Long> posViejas = cacheTipoSangrePosiciones.get(tipoSangreViejo);
            if (posViejas != null) {
                posViejas.remove(posicionFisica);
                indexTipoSangreHandler.eliminar(tipoSangreViejo, posicionFisica);
            }

            //* Agregar al tipo de sangre nuevo
            cacheTipoSangrePosiciones.computeIfAbsent(
                    tipoSangreNuevo,
                    k -> new ArrayList<>()
            ).add(posicionFisica);
            indexTipoSangreHandler.agregar(tipoSangreNuevo, posicionFisica);
        }

        //* Actualizar caché (la identificación no cambia)
        cacheIdentificacionPosicion.put(identificacion, posicionFisica);
    }

    public void eliminarPaciente(String identificacion) throws Exception {
        Long posicionFisica = cacheIdentificacionPosicion.get(identificacion);

        if (posicionFisica == null) {
            throw new Exception("Paciente no encontrado con identificación: " + identificacion);
        }

        //* Obtener tipo de sangre antes de eliminar
        Paciente paciente = buscarPorIdentificacion(identificacion);
        if (paciente == null) {
            throw new Exception("Paciente no encontrado con identificación: " + identificacion);
        }

        //* Marcar como libre en el archivo de anillo
        int posicionRegistro = (int) (posicionFisica / (1 + serializador.getTamanioRegistro()));
        ringHandler.liberarEspacio(posicionRegistro);

        //* Limpiar caché
        cacheIdentificacionPosicion.remove(identificacion);
    }

    public boolean existePaciente(String identificacion) throws Exception {
        return buscarPorIdentificacion(identificacion) != null;
    }

    // MÉTODOS UTILITARIOS

    /**
     * Obtiene todos los tipos de sangre disponibles
     */
    public Set<String> obtenerTiposSangre() throws Exception {
        return new HashSet<>(cacheTipoSangrePosiciones.keySet());
    }

    /**
     * Cuenta cuántos pacientes hay por tipo de sangre
     */
    public Map<String, Long> contarPacientesPorTipoSangre() throws Exception {
        Map<String, Long> conteo = new HashMap<>();
        List<Paciente> pacientes = listarTodos();

        for (Paciente paciente : pacientes) {
            String tipo = paciente.getTipoSangre();
            conteo.put(tipo, conteo.getOrDefault(tipo, 0L) + 1);
        }

        return conteo;
    }

    /**
     * Recarga la caché desde los archivos (útil si hubo cambios externos)
     */
    public void recargarCache() throws Exception {
        cargarCache();
    }
}