package com.paboomi.frontend.features.pacientes;

import com.paboomi.backend.dto.PacienteDTO;
import com.paboomi.frontend.facade.ClinicaFacade;
import com.paboomi.frontend.shared.DataTablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PacienteView extends JPanel {

    private DataTablePanel tablaPacientes;
    private JTextField txtBuscarIdentificacion;
    private JTextField txtBuscarNombre;
    private JButton btnBuscar;
    private JButton btnLimpiar;
    private JButton btnNuevo;
    private JButton btnEliminar;

    private List<PacienteDTO> listaActual;

    public PacienteView() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        initToolbar();
        initTabla();
        cargarDatos();
    }

    /**
     * Barra superior con acciones principales y campos de filtrado
     */
    private void initToolbar() {
        JPanel pnlTop = new JPanel(new BorderLayout(0, 10));


        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnNuevo = new JButton("Nuevo Paciente");
        btnEliminar = new JButton("Eliminar Seleccionado");

        pnlAcciones.add(btnNuevo);
        pnlAcciones.add(btnEliminar);


        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        txtBuscarIdentificacion = new JTextField(12);
        txtBuscarNombre = new JTextField(15);
        btnBuscar = new JButton("Buscar");
        btnLimpiar = new JButton("Ver Todos");

        pnlFiltros.add(new JLabel("DPI / Identificación:"));
        pnlFiltros.add(txtBuscarIdentificacion);
        pnlFiltros.add(new JLabel("Nombre / Apellido:"));
        pnlFiltros.add(txtBuscarNombre);
        pnlFiltros.add(btnBuscar);
        pnlFiltros.add(btnLimpiar);

        pnlTop.add(pnlAcciones, BorderLayout.NORTH);
        pnlTop.add(pnlFiltros, BorderLayout.SOUTH);

        add(pnlTop, BorderLayout.NORTH);


        btnNuevo.addActionListener(e -> abrirModalNuevo());
        btnEliminar.addActionListener(e -> eliminarSeleccionado());
        btnBuscar.addActionListener(e -> aplicarFiltro());
        btnLimpiar.addActionListener(e -> {
            txtBuscarIdentificacion.setText("");
            txtBuscarNombre.setText("");
            cargarDatos();
        });
    }

    /**
     * Inicializa el DataTablePanel con las columnas del DTO de Pacientes
     */
    private void initTabla() {
        String[] columnas = {
                "Identificación", "Nombres", "Apellidos",
                "Fecha Nacimiento", "Sexo", "Teléfono", "Email", "Tipo Sangre"
        };

        tablaPacientes = new DataTablePanel(columnas);
        add(tablaPacientes, BorderLayout.CENTER);
    }

    /**
     * Carga o refresca todos los pacientes desde el Facade
     */
    public void cargarDatos() {
        try {
            listaActual = ClinicaFacade.getInstance().listarPacientes();
            mostrarEnTabla(listaActual);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar pacientes: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Mapea la lista de PacienteDTO a filas de la tabla
     */
    private void mostrarEnTabla(List<PacienteDTO> lista) {
        List<Object[]> filas = new ArrayList<>();
        if (lista != null) {
            for (PacienteDTO p : lista) {
                filas.add(new Object[]{
                        p.getIdentificacion(),
                        p.getNombres(),
                        p.getApellidos(),
                        p.getFechaNacimiento(),
                        p.getSexo(),
                        p.getTelefono(),
                        p.getEmail() != null ? p.getEmail() : "",
                        p.getTipoSangre()
                });
            }
        }
        tablaPacientes.actualizarDatos(filas);
    }

    /**
     * Abre la ventana modal para registrar un nuevo paciente
     */
    private void abrirModalNuevo() {
        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        PacienteFormDialog dialog = new PacienteFormDialog(topFrame, ClinicaFacade.getInstance().getPacienteService());
        dialog.setVisible(true);
        cargarDatos();
    }

    /**
     * Aplica búsqueda por Identificación o Nombre
     */
    private void aplicarFiltro() {
        String id = txtBuscarIdentificacion.getText().trim();
        String nombre = txtBuscarNombre.getText().trim();

        try {
            if (!id.isEmpty()) {
                //* Buscar por identificación única
                PacienteDTO p = ClinicaFacade.getInstance().buscarPacientePorIdentificacion(id);
                listaActual = (p != null) ? List.of(p) : new ArrayList<>();
            } else if (!nombre.isEmpty()) {
                //* Búsqueda por coincidencia de nombre
                listaActual = ClinicaFacade.getInstance().buscarPacientesPorNombre(nombre);
            } else {
                cargarDatos();
                return;
            }
            mostrarEnTabla(listaActual);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error en Búsqueda", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Realiza el Soft Delete del registro seleccionado en la tabla
     */
    private void eliminarSeleccionado() {
        int fila = tablaPacientes.getFilaSeleccionada();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un paciente de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) tablaPacientes.getValorCelda(fila, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Desea eliminar al paciente con identificación: " + id + "?",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                ClinicaFacade.getInstance().getPacienteService().eliminarPaciente(id);
                JOptionPane.showMessageDialog(this, "Paciente eliminado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarDatos();
            } catch (Exception e) {
                //* Atrapa excepciones de validación
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error al Eliminar", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
