package com.paboomi.backend.util.ring;

import com.paboomi.backend.models.Paciente;
import com.paboomi.backend.util.RandomAccessFileUtil;

import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Serializador para la entidad Paciente
 * Maneja la conversión entre objetos Paciente y bytes en el archivo
 */
public class PacienteSerializador implements Serializador<Paciente> {

    // Constantes de tamaño para campos de texto
    private static final int LONGITUD_IDENTIFICACION = 20;
    private static final int LONGITUD_NOMBRES = 50;
    private static final int LONGITUD_APELLIDOS = 50;
    private static final int LONGITUD_FECHA = 10;  // yyyy-MM-dd
    private static final int LONGITUD_SEXO = 1;
    private static final int LONGITUD_TELEFONO = 15;
    private static final int LONGITUD_EMAIL = 50;
    private static final int LONGITUD_TIPO_SANGRE = 3;

    // Formato de fecha
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // Tamaño total del registro de datos
    private static final int TAMANIO_REGISTRO_DATOS =
            (LONGITUD_IDENTIFICACION * 2) +
                    (LONGITUD_NOMBRES * 2) +
                    (LONGITUD_APELLIDOS * 2) +
                    (LONGITUD_FECHA * 2) +
                    (LONGITUD_SEXO * 2) +
                    (LONGITUD_TELEFONO * 2) +
                    (LONGITUD_EMAIL * 2) +
                    (LONGITUD_TIPO_SANGRE * 2);

    @Override
    public void escribir(RandomAccessFile raf, Paciente paciente) throws Exception {
        // Identificación
        RandomAccessFileUtil.escribirCadenaFija(raf, paciente.getIdentificacion(), LONGITUD_IDENTIFICACION);

        // Nombres
        RandomAccessFileUtil.escribirCadenaFija(raf, paciente.getNombres(), LONGITUD_NOMBRES);

        // Apellidos
        RandomAccessFileUtil.escribirCadenaFija(raf, paciente.getApellidos(), LONGITUD_APELLIDOS);

        // Fecha de nacimiento (formateada)
        String fechaStr = DATE_FORMAT.format(paciente.getFechaNacimiento());
        RandomAccessFileUtil.escribirCadenaFija(raf, fechaStr, LONGITUD_FECHA);

        // Sexo
        RandomAccessFileUtil.escribirCadenaFija(raf, String.valueOf(paciente.getSexo()), LONGITUD_SEXO);

        // Teléfono
        RandomAccessFileUtil.escribirCadenaFija(raf, paciente.getNumeroTelefono(), LONGITUD_TELEFONO);

        // Email
        String email = paciente.getEmail() != null ? paciente.getEmail() : "";
        RandomAccessFileUtil.escribirCadenaFija(raf, email, LONGITUD_EMAIL);

        // Tipo de sangre
        RandomAccessFileUtil.escribirCadenaFija(raf, paciente.getTipoSangre(), LONGITUD_TIPO_SANGRE);
    }

    @Override
    public Paciente leer(RandomAccessFile raf) throws Exception {
        // Leer identificación
        String identificacion = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_IDENTIFICACION);

        // Leer nombres
        String nombres = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_NOMBRES);

        // Leer apellidos
        String apellidos = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_APELLIDOS);

        // Leer fecha de nacimiento
        String fechaStr = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_FECHA);
        Date fechaNacimiento = DATE_FORMAT.parse(fechaStr);

        // Leer sexo
        String sexoStr = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_SEXO);
        char sexo = sexoStr.charAt(0);

        // Leer teléfono
        String telefono = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_TELEFONO);

        // Leer email
        String email = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_EMAIL);
        if (email.isEmpty()) {
            email = null;
        }

        // Leer tipo de sangre
        String tipoSangre = RandomAccessFileUtil.leerCadenaFija(raf, LONGITUD_TIPO_SANGRE);

        // Construir y retornar el objeto Paciente
        return new Paciente(
                identificacion,
                nombres,
                apellidos,
                fechaNacimiento,
                sexo,
                telefono,
                email,
                tipoSangre
        );
    }

    @Override
    public int getTamanioRegistro() {
        return TAMANIO_REGISTRO_DATOS;
    }
}