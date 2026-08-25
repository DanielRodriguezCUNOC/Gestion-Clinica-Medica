package com.paboomi.backend.util.ring;

import com.paboomi.backend.models.Cita;
import com.paboomi.backend.util.RandomAccessFileUtil;

import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Serializador para la entidad Cita
 * Maneja la conversión entre objetos Cita y bytes en el archivo
 */
public class CitaSerializador implements Serializador<Cita> {

    // Constantes de tamaño para campos de texto
    private static final int LONGITUD_UUID = 36;
    private static final int LONGITUD_ID_PACIENTE = 20;
    private static final int LONGITUD_UUID_MEDICO = 36;
    private static final int LONGITUD_FECHA = 10;      // yyyy-MM-dd
    private static final int LONGITUD_HORA = 5;        // HH:mm
    private static final int LONGITUD_MOTIVO = 100;
    private static final int LONGITUD_ESTADO = 15;
    private static final int LONGITUD_OBSERVACIONES = 200;

    // Tamaño total del registro de datos
    private static final int TAMANIO_REGISTRO_DATOS =
            (LONGITUD_UUID * 2) +
                    (LONGITUD_ID_PACIENTE * 2) +
                    (LONGITUD_UUID_MEDICO * 2) +
                    (LONGITUD_FECHA * 2) +
                    (LONGITUD_HORA * 2) +
                    (LONGITUD_MOTIVO * 2) +
                    (LONGITUD_ESTADO * 2) +
                    (LONGITUD_OBSERVACIONES * 2);

    @Override
    public void escribir(RandomAccessFile raf, Cita cita) throws Exception {
        // UUID de la cita
        RandomAccessFileUtil.escribirCadenaFija(raf, cita.getId().toString(), LONGITUD_UUID);

        // Identificación del paciente
        RandomAccessFileUtil.escribirCadenaFija(raf, cita.getIdPaciente(), LONGITUD_ID_PACIENTE);

        // UUID del médico
        RandomAccessFileUtil.escribirCadenaFija(raf, cita.getIdMedico().toString(), LONGITUD_UUID_MEDICO);

        // Fecha de la cita
        RandomAccessFileUtil.escribirCadenaFija(raf, cita.getFechaCita().toString(), LONGITUD_FECHA);

        // Hora de inicio
        RandomAccessFileUtil.escribirCadenaFija(raf, cita.getHoraInicio().toString(), LONGITUD_HORA);

        // Motivo de la consulta
        RandomAccessFileUtil.escribirCadenaFija(raf, cita.getMotivo(), LONGITUD_MOTIVO);

        // Estado de la cita
        RandomAccessFileUtil.escribirCadenaFija(raf, cita.getEstado(), LONGITUD_ESTADO);

        // Observaciones
        String observaciones = cita.getObservaciones() != null ? cita.getObservaciones() : "";
        RandomAccessFileUtil.escribirCadenaFija(raf, observaciones, LONGITUD_OBSERVACIONES);
    }

    @Override
    public Cita leer(RandomAccessFile raf) throws Exception {
        // Leer UUID de la cita
        String idStr = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_UUID);
        UUID id = UUID.fromString(idStr);

        // Leer identificación del paciente
        String idPaciente = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_ID_PACIENTE);

        // Leer UUID del médico
        String idMedicoStr = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_UUID_MEDICO);
        UUID idMedico = UUID.fromString(idMedicoStr);

        // Leer fecha
        String fechaStr = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_FECHA);
        LocalDate fecha = LocalDate.parse(fechaStr);

        // Leer hora de inicio
        String horaStr = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_HORA);
        LocalTime horaInicio = LocalTime.parse(horaStr);

        // Leer motivo
        String motivo = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_MOTIVO);

        // Leer estado
        String estado = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_ESTADO);

        // Leer observaciones
        String observaciones = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_OBSERVACIONES);
        if (observaciones.isEmpty()) {
            observaciones = null;
        }

        // Construir y retornar el objeto Cita
        return new Cita(
                id,
                idPaciente,
                idMedico,
                fecha,
                horaInicio,
                motivo,
                estado,
                observaciones
        );
    }

    @Override
    public int getTamanioRegistro() {
        return TAMANIO_REGISTRO_DATOS;
    }
}