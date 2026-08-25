package com.paboomi.frontend.features.citas;

import com.paboomi.backend.dto.CitaDTO;
import com.paboomi.frontend.facade.ClinicaFacade;
import com.paboomi.frontend.shared.DataTablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CitaView extends JPanel {

    private DataTablePanel tablaCitas;
    private JTextField txtBuscarPaciente;
    private JTextField txtBuscarFecha;
    private JComboBox<String> cmbFiltroEstado;
    private JButton btnBuscar;
    private JButton btnLimpiar;

    private JButton btnProgramar;
    private JButton btnMarcarAtendida;
    private JButton btnCancelar;
    private JButton btnEliminar;

    private List<CitaDTO> listaActual;

    public CitaView() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        initToolbar();
        initTabla();
        cargarDatos();
    }

    private void initToolbar() {
        JPanel pnlTop = new JPanel(new BorderLayout(0, 10));

        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnProgramar = new JButton("Programar Cita");
        btnMarcarAtendida = new JButton("Marcar Atendida");
        btnCancelar = new JButton("Cancelar Cita");
        btnEliminar = new JButton("Eliminar Cita");

        pnlAcciones.add(btnProgramar);
        pnlAcciones.add(btnMarcarAtendida);
        pnlAcciones.add(btnCancelar);
        pnlAcciones.add(btnEliminar);

        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        txtBuscarPaciente = new JTextField(10);
        txtBuscarFecha = new JTextField(8);
        txtBuscarFecha.setToolTipText("yyyy-MM-dd");
        cmbFiltroEstado = new JComboBox<>(new String[]{"Todos", "Programada", "Atendida", "Cancelada"});
        btnBuscar = new JButton("Filtrar");
        btnLimpiar = new JButton("Ver Todas");

        pnlFiltros.add(new JLabel("DPI Paciente:"));
        pnlFiltros.add(txtBuscarPaciente);
        pnlFiltros.add(new JLabel("Fecha:"));
        pnlFiltros.add(txtBuscarFecha);
        pnlFiltros.add(new JLabel("Estado:"));
        pnlFiltros.add(cmbFiltroEstado);
        pnlFiltros.add(btnBuscar);
        pnlFiltros.add(btnLimpiar);

        pnlTop.add(pnlAcciones, BorderLayout.NORTH);
        pnlTop.add(pnlFiltros, BorderLayout.SOUTH);
        add(pnlTop, BorderLayout.NORTH);

        btnProgramar.addActionListener(e -> abrirModalNuevo());
        btnMarcarAtendida.addActionListener(e -> cambiarEstadoCita(true));
        btnCancelar.addActionListener(e -> cambiarEstadoCita(false));
        btnEliminar.addActionListener(e -> eliminarSeleccionada());
        btnBuscar.addActionListener(e -> aplicarFiltros());
        btnLimpiar.addActionListener(e -> {
            txtBuscarPaciente.setText("");
            txtBuscarFecha.setText("");
            cmbFiltroEstado.setSelectedIndex(0);
            cargarDatos();
        });
    }

    private void initTabla() {
        String[] columnas = {
                "ID Cita", "DPI Paciente", "Paciente", "ID Médico",
                "Médico", "Especialidad", "Fecha", "Hora", "Estado", "Motivo", "Observaciones"
        };

        tablaCitas = new DataTablePanel(columnas);
        add(tablaCitas, BorderLayout.CENTER);
    }

    public void cargarDatos() {
        try {
            listaActual = ClinicaFacade.getInstance().listarCitas();
            mostrarEnTabla(listaActual);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar citas: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarEnTabla(List<CitaDTO> lista) {
        List<Object[]> filas = new ArrayList<>();
        if (lista != null) {
            for (CitaDTO c : lista) {
                filas.add(new Object[]{
                        c.getId(),
                        c.getIdentificacionPaciente(),
                        c.getNombrePaciente(),
                        c.getIdMedico(),
                        c.getNombreMedico(),
                        c.getEspecialidadMedico(),
                        c.getFecha(),
                        c.getHoraInicio(),
                        c.getEstado(),
                        c.getMotivo(),
                        c.getObservaciones() != null ? c.getObservaciones() : ""
                });
            }
        }
        tablaCitas.actualizarDatos(filas);
    }

    private void abrirModalNuevo() {
        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        CitaFormDialog dialog = new CitaFormDialog(topFrame);
        dialog.setVisible(true);
        cargarDatos();
    }

    private void cambiarEstadoCita(boolean esAtendida) {
        int fila = tablaCitas.getFilaSeleccionada();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String idStr = (String) tablaCitas.getValorCelda(fila, 0);
        try {
            UUID idCita = UUID.fromString(idStr);
            if (esAtendida) {
                ClinicaFacade.getInstance().getCitaService().marcarComoAtendida(idCita);
                JOptionPane.showMessageDialog(this, "Cita marcada como Atendida.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                ClinicaFacade.getInstance().getCitaService().cancelarCita(idCita);
                JOptionPane.showMessageDialog(this, "Cita cancelada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
            cargarDatos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error de Operación", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarSeleccionada() {
        int fila = tablaCitas.getFilaSeleccionada();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String idStr = (String) tablaCitas.getValorCelda(fila, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar cita permanentemente?", "Confirmar", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                ClinicaFacade.getInstance().getCitaService().eliminarCita(UUID.fromString(idStr));
                JOptionPane.showMessageDialog(this, "Cita eliminada correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarDatos();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error al Eliminar", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void aplicarFiltros() {
        String paciente = txtBuscarPaciente.getText().trim();
        String fechaStr = txtBuscarFecha.getText().trim();
        String estado = (String) cmbFiltroEstado.getSelectedItem();

        try {
            if (!paciente.isEmpty()) {
                listaActual = ClinicaFacade.getInstance().getCitaService().buscarPorPaciente(paciente);
            } else if (!fechaStr.isEmpty()) {
                java.time.LocalDate fecha = java.time.LocalDate.parse(fechaStr);
                listaActual = ClinicaFacade.getInstance().getCitaService().buscarPorFecha(fecha);
            } else if (!"Todos".equals(estado)) {
                listaActual = ClinicaFacade.getInstance().getCitaService().buscarPorEstado(estado);
            } else {
                cargarDatos();
                return;
            }
            mostrarEnTabla(listaActual);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error en filtrado: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}