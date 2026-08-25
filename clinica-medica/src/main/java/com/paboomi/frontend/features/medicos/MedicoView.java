package com.paboomi.frontend.features.medicos;

import com.paboomi.backend.dto.MedicoDTO;
import com.paboomi.frontend.facade.ClinicaFacade;
import com.paboomi.frontend.shared.DataTablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MedicoView extends JPanel {

    private DataTablePanel tablaMedicos;
    private JTextField txtBuscarNombre;
    private JTextField txtBuscarEspecialidad;
    private JComboBox<String> cmbFiltroEstado;
    private JButton btnBuscar;
    private JButton btnLimpiar;
    private JButton btnNuevo;
    private JButton btnCambiarEstado;

    private List<MedicoDTO> listaActual;

    public MedicoView() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        initToolbar();
        initTabla();
        cargarDatos();
    }

    private void initToolbar() {
        JPanel pnlTop = new JPanel(new BorderLayout(0, 10));

        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnNuevo = new JButton("Nuevo Médico");
        btnCambiarEstado = new JButton("Activar / Desactivar");

        pnlAcciones.add(btnNuevo);
        pnlAcciones.add(btnCambiarEstado);

        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        txtBuscarNombre = new JTextField(12);
        txtBuscarEspecialidad = new JTextField(12);
        cmbFiltroEstado = new JComboBox<>(new String[]{"Todos", "Activos", "Inactivos"});
        btnBuscar = new JButton("Buscar");
        btnLimpiar = new JButton("Ver Todos");

        pnlFiltros.add(new JLabel("Nombre/Apellido:"));
        pnlFiltros.add(txtBuscarNombre);
        pnlFiltros.add(new JLabel("Especialidad:"));
        pnlFiltros.add(txtBuscarEspecialidad);
        pnlFiltros.add(new JLabel("Estado:"));
        pnlFiltros.add(cmbFiltroEstado);
        pnlFiltros.add(btnBuscar);
        pnlFiltros.add(btnLimpiar);

        pnlTop.add(pnlAcciones, BorderLayout.NORTH);
        pnlTop.add(pnlFiltros, BorderLayout.SOUTH);

        add(pnlTop, BorderLayout.NORTH);

        btnNuevo.addActionListener(e -> abrirModalNuevo());
        btnCambiarEstado.addActionListener(e -> alternarEstadoSeleccionado());
        btnBuscar.addActionListener(e -> aplicarFiltros());
        btnLimpiar.addActionListener(e -> {
            txtBuscarNombre.setText("");
            txtBuscarEspecialidad.setText("");
            cmbFiltroEstado.setSelectedIndex(0);
            cargarDatos();
        });
    }

    private void initTabla() {
        String[] columnas = {
                "UUID", "Nombre Completo", "Especialidad",
                "Teléfono", "Correo", "Horario Inicio", "Horario Fin", "Estado"
        };

        tablaMedicos = new DataTablePanel(columnas);
        add(tablaMedicos, BorderLayout.CENTER);
    }

    public void cargarDatos() {
        try {
            listaActual = ClinicaFacade.getInstance().listarMedicos();
            mostrarEnTabla(listaActual);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar médicos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarEnTabla(List<MedicoDTO> lista) {
        List<Object[]> filas = new ArrayList<>();
        if (lista != null) {
            for (MedicoDTO m : lista) {
                filas.add(new Object[]{
                        m.getId(),
                        m.getNombresCompletos(),
                        m.getEspecialidad(),
                        m.getTelefono(),
                        m.getCorreo() != null ? m.getCorreo() : "",
                        m.getHorarioInicioAtencion(),
                        m.getHorarioFinAtencion(),
                        m.getEstado()
                });
            }
        }
        tablaMedicos.actualizarDatos(filas);
    }

    private void abrirModalNuevo() {
        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        MedicoFormDialog dialog = new MedicoFormDialog(topFrame, ClinicaFacade.getInstance().getMedicoService());
        dialog.setVisible(true);
        cargarDatos();
    }

    private void aplicarFiltros() {
        String nombre = txtBuscarNombre.getText().trim();
        String especialidad = txtBuscarEspecialidad.getText().trim();
        String estado = (String) cmbFiltroEstado.getSelectedItem();

        try {
            if (!nombre.isEmpty()) {
                listaActual = ClinicaFacade.getInstance().getMedicoService().buscarPorNombre(nombre);
            } else if (!especialidad.isEmpty()) {
                listaActual = ClinicaFacade.getInstance().getMedicoService().buscarPorEspecialidad(especialidad);
            } else if ("Activos".equals(estado)) {
                listaActual = ClinicaFacade.getInstance().getMedicoService().listarActivos();
            } else if ("Inactivos".equals(estado)) {
                listaActual = ClinicaFacade.getInstance().getMedicoService().listarInactivos();
            } else {
                cargarDatos();
                return;
            }
            mostrarEnTabla(listaActual);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error en Búsqueda", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void alternarEstadoSeleccionado() {
        int fila = tablaMedicos.getFilaSeleccionada();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un médico de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String uuidStr = (String) tablaMedicos.getValorCelda(fila, 0);
        String estadoActual = (String) tablaMedicos.getValorCelda(fila, 7);

        try {
            java.util.UUID id = java.util.UUID.fromString(uuidStr);
            if ("Activo".equalsIgnoreCase(estadoActual)) {
                ClinicaFacade.getInstance().getMedicoService().desactivarMedico(id);
            } else {
                ClinicaFacade.getInstance().getMedicoService().activarMedico(id);
            }
            cargarDatos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error al cambiar estado", JOptionPane.ERROR_MESSAGE);
        }
    }
}
