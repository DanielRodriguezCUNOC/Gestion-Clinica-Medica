package com.paboomi.backend.util.ring;

import com.paboomi.backend.models.Medico;
import com.paboomi.backend.util.files.RandomAccessFileUtil;

import java.io.RandomAccessFile;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Serializador para la entidad Medico
 * Maneja la conversión entre objetos Medico y bytes en el archivo
 */
public class MedicoSerializador implements Serializador<Medico> {

    // Constantes de tamaño para campos de texto
    private static final int LONGITUD_UUID = 36;
    private static final int LONGITUD_NOMBRES = 50;
    private static final int LONGITUD_APELLIDOS = 50;
    private static final int LONGITUD_ESPECIALIDAD = 40;
    private static final int LONGITUD_TELEFONO = 15;
    private static final int LONGITUD_CORREO = 50;
    private static final int LONGITUD_HORA = 5;

    // Tamaño total del registro de datos (sin incluir el bitmap)
    private static final int TAMANIO_REGISTRO_DATOS =
            (LONGITUD_UUID * 2) +
                    (LONGITUD_NOMBRES * 2) +
                    (LONGITUD_APELLIDOS * 2) +
                    (LONGITUD_ESPECIALIDAD * 2) +
                    (LONGITUD_TELEFONO * 2) +
                    (LONGITUD_CORREO * 2) +
                    (LONGITUD_HORA * 2) +
                    (LONGITUD_HORA * 2) +
                    1; // boolean activo

    @Override
    public void escribir(RandomAccessFile raf, Medico medico) throws Exception {
        // UUID
        RandomAccessFileUtil.escribirCadenaFija(raf, medico.getId().toString(), LONGITUD_UUID);

        // Nombres
        RandomAccessFileUtil.escribirCadenaFija(raf, medico.getNombres(), LONGITUD_NOMBRES);

        // Apellidos
        RandomAccessFileUtil.escribirCadenaFija(raf, medico.getApellidos(), LONGITUD_APELLIDOS);

        // Especialidad
        RandomAccessFileUtil.escribirCadenaFija(raf, medico.getEspecialidad(), LONGITUD_ESPECIALIDAD);

        // Teléfono
        RandomAccessFileUtil.escribirCadenaFija(raf, medico.getTelefono(), LONGITUD_TELEFONO);

        // Correo electrónico
        String correo = medico.getCorreoElectronico() != null ? medico.getCorreoElectronico() : "";
        RandomAccessFileUtil.escribirCadenaFija(raf, correo, LONGITUD_CORREO);

        // Horario inicio
        RandomAccessFileUtil.escribirCadenaFija(raf, medico.getHorarioInicio().toString(), LONGITUD_HORA);

        // Horario fin
        RandomAccessFileUtil.escribirCadenaFija(raf, medico.getHorarioFin().toString(), LONGITUD_HORA);

        // Estado activo
        raf.writeBoolean(medico.isActivo());
    }

    @Override
    public Medico leer(RandomAccessFile raf) throws Exception {
        // Leer UUID
        String idStr = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_UUID);
        UUID id = UUID.fromString(idStr);

        // Leer nombres
        String nombres = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_NOMBRES);

        // Leer apellidos
        String apellidos = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_APELLIDOS);

        // Leer especialidad
        String especialidad = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_ESPECIALIDAD);

        // Leer teléfono
        String telefono = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_TELEFONO);

        // Leer correo
        String correo = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_CORREO);
        if (correo.isEmpty()) {
            correo = null;
        }

        // Leer horario inicio
        String horaInicioStr = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_HORA);
        LocalTime horarioInicio = LocalTime.parse(horaInicioStr);

        // Leer horario fin
        String horaFinStr = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_HORA);
        LocalTime horarioFin = LocalTime.parse(horaFinStr);

        // Leer estado activo
        boolean activo = raf.readBoolean();

        // Construir y retornar el objeto Medico
        return new Medico(
                id,
                nombres,
                apellidos,
                especialidad,
                telefono,
                correo,
                horarioInicio,
                horarioFin,
                activo
        );
    }

    @Override
    public int getTamanioRegistro() {
        return TAMANIO_REGISTRO_DATOS;
    }
}