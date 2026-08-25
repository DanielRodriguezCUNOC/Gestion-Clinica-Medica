package com.paboomi.frontend.features.reportes;

import com.paboomi.backend.models.LogEntry;
import com.paboomi.backend.services.LogService;
import com.paboomi.backend.util.exceptions.ServiceException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Vista para visualizar y gestionar logs del sistema
 * Permite filtrar por fecha, módulo, acción y usuario
 */
public class ReporteLogsView extends JPanel {

    private final LogService logService;
    private JTable tablaLogs;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JComboBox<String> comboModulo;
    private JComboBox<String> comboAccion;
    private JTextField txtUsuario;
    private JButton btnFiltrar;
    private JButton btnLimpiarFiltros;
    private JButton btnExportarCSV;
    private JButton btnActualizar;
    private JButton btnLimpiarAntiguos;
    private JLabel lblTotalRegistros;
    private JPanel panelFiltros;
    private JPanel panelBotones;
    private JScrollPane scrollPane;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public ReporteLogsView(LogService logService) {
        this.logService = logService;
        initComponents();
        cargarLogs();
    }

    //* INICIALIZACIÓN DE COMPONENTES
    private void initComponents() {

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelFiltros = crearPanelFiltros();
        add(panelFiltros, BorderLayout.NORTH);

        crearTablaLogs();
        scrollPane = new JScrollPane(tablaLogs);
        scrollPane.setPreferredSize(new Dimension(800, 400));
        add(scrollPane, BorderLayout.CENTER);

        panelBotones = crearPanelBotones();
        add(panelBotones, BorderLayout.SOUTH);
    }

    //* PANEL DE FILTROS
    private JPanel crearPanelFiltros() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Filtros de Búsqueda"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Módulo:"), gbc);

        gbc.gridx = 1;
        comboModulo = new JComboBox<>();
        comboModulo.addItem("Todos");
        for (LogEntry.Modulo modulo : LogEntry.Modulo.values()) {
            comboModulo.addItem(modulo.toString());
        }
        comboModulo.setPreferredSize(new Dimension(120, 25));
        panel.add(comboModulo, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Acción:"), gbc);

        gbc.gridx = 3;
        comboAccion = new JComboBox<>();
        comboAccion.addItem("Todos");
        for (LogEntry.Accion accion : LogEntry.Accion.values()) {
            comboAccion.addItem(accion.toString());
        }
        comboAccion.setPreferredSize(new Dimension(120, 25));
        panel.add(comboAccion, gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("Usuario:"), gbc);

        gbc.gridx = 5;
        txtUsuario = new JTextField(15);
        txtUsuario.setToolTipText("Nombre de usuario (coincidencia parcial)");
        panel.add(txtUsuario, gbc);

        gbc.gridx = 6;
        btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(e -> aplicarFiltros());
        panel.add(btnFiltrar, gbc);

        gbc.gridx = 7;
        btnLimpiarFiltros = new JButton("Limpiar");
        btnLimpiarFiltros.addActionListener(e -> limpiarFiltros());
        panel.add(btnLimpiarFiltros, gbc);

        return panel;
    }

    //* TABLA DE LOGS
    private void crearTablaLogs() {
        // Definir columnas
        String[] columnas = {"Fecha/Hora", "Usuario", "Módulo", "Acción", "Detalle", "ID"};

        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaLogs = new JTable(tableModel);
        tablaLogs.setRowHeight(25);
        tablaLogs.setFont(new Font("Monospaced", Font.PLAIN, 12));

        sorter = new TableRowSorter<>(tableModel);
        tablaLogs.setRowSorter(sorter);

        tablaLogs.getColumnModel().getColumn(0).setPreferredWidth(150);  // Fecha/Hora
        tablaLogs.getColumnModel().getColumn(1).setPreferredWidth(100);  // Usuario
        tablaLogs.getColumnModel().getColumn(2).setPreferredWidth(100);  // Módulo
        tablaLogs.getColumnModel().getColumn(3).setPreferredWidth(100);  // Acción
        tablaLogs.getColumnModel().getColumn(4).setPreferredWidth(400);  // Detalle
        tablaLogs.getColumnModel().getColumn(5).setPreferredWidth(150);  // ID

        //* Ver detalles completos
        tablaLogs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                int row = tablaLogs.rowAtPoint(evt.getPoint());
                if (row >= 0) {
                    String detalle = (String) tableModel.getValueAt(row, 4);
                    tablaLogs.setToolTipText(detalle);
                }
            }
        });
    }

    //* PANEL DE BOTONES INFERIOR
    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JPanel panelEstadisticas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblTotalRegistros = new JLabel("Total: 0 registros");
        lblTotalRegistros.setFont(new Font("Arial", Font.BOLD, 12));
        panelEstadisticas.add(lblTotalRegistros);
        panel.add(panelEstadisticas, BorderLayout.WEST);

        JPanel panelBotonesAccion = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> cargarLogs());
        panelBotonesAccion.add(btnActualizar);

        btnExportarCSV = new JButton("Exportar CSV");
        btnExportarCSV.addActionListener(e -> exportarCSV());
        panelBotonesAccion.add(btnExportarCSV);

        btnLimpiarAntiguos = new JButton("Limpiar Antiguos");
        btnLimpiarAntiguos.addActionListener(e -> limpiarLogsAntiguos());
        panelBotonesAccion.add(btnLimpiarAntiguos);

        panel.add(panelBotonesAccion, BorderLayout.EAST);

        return panel;
    }

    //* MÉTODOS DE CARGA DE DATOS
    private void cargarLogs() {
        try {

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            List<LogEntry> logs = logService.obtenerTodosLosLogs();
            actualizarTabla(logs);

            lblTotalRegistros.setText("Total: " + logs.size() + " registros");

            setCursor(Cursor.getDefaultCursor());
        } catch (ServiceException e) {
            setCursor(Cursor.getDefaultCursor());
            JOptionPane.showMessageDialog(this,
                    "Error al cargar logs: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarTabla(List<LogEntry> logs) {

        tableModel.setRowCount(0);

        for (LogEntry log : logs) {
            tableModel.addRow(new Object[]{
                    log.getFechaHora().format(DATE_FORMATTER),
                    log.getUsuario() != null ? log.getUsuario() : "SISTEMA",
                    log.getModulo(),
                    log.getAccion(),
                    log.getDetalle() != null ? log.getDetalle() : "",
                    log.getIdEntidad() != null ? log.getIdEntidad() : ""
            });
        }
    }

    //* MÉTODOS DE FILTRADO
    private void aplicarFiltros() {
        try {
            String modulo = comboModulo.getSelectedItem().toString();
            String accion = comboAccion.getSelectedItem().toString();
            String usuario = txtUsuario.getText().trim();

            List<LogEntry> logsFiltrados = logService.obtenerTodosLosLogs();

            //* Filtrar por módulo
            if (!modulo.equals("Todos")) {
                LogEntry.Modulo mod = LogEntry.Modulo.valueOf(modulo);
                logsFiltrados = logsFiltrados.stream()
                        .filter(log -> log.getModulo() == mod)
                        .collect(java.util.stream.Collectors.toList());
            }

            //* Filtrar por acción
            if (!accion.equals("Todos")) {
                LogEntry.Accion act = LogEntry.Accion.valueOf(accion);
                logsFiltrados = logsFiltrados.stream()
                        .filter(log -> log.getAccion() == act)
                        .collect(java.util.stream.Collectors.toList());
            }

            //* Filtrar por usuario
            if (!usuario.isEmpty()) {
                logsFiltrados = logsFiltrados.stream()
                        .filter(log -> log.getUsuario() != null &&
                                log.getUsuario().toLowerCase().contains(usuario.toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());
            }

            actualizarTabla(logsFiltrados);
            lblTotalRegistros.setText("Total: " + logsFiltrados.size() + " registros (filtrados)");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al filtrar logs: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFiltros() {
        comboModulo.setSelectedIndex(0);
        comboAccion.setSelectedIndex(0);
        txtUsuario.setText("");
        cargarLogs();
    }

    //* MÉTODOS DE EXPORTACIÓN
    private void exportarCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar archivo CSV");
        fileChooser.setSelectedFile(new File("logs_" + LocalDate.now() + ".csv"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String ruta = fileChooser.getSelectedFile().getAbsolutePath();
                if (!ruta.endsWith(".csv")) {
                    ruta += ".csv";
                }

                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                logService.exportarLogsACSV(ruta);
                setCursor(Cursor.getDefaultCursor());

                JOptionPane.showMessageDialog(this,
                        "Logs exportados correctamente a:\n" + ruta,
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (ServiceException e) {
                setCursor(Cursor.getDefaultCursor());
                JOptionPane.showMessageDialog(this,
                        "Error al exportar logs: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    //* MÉTODOS DE MANTENIMIENTO
    private void limpiarLogsAntiguos() {
        // Preguntar cuántos días mantener
        String input = JOptionPane.showInputDialog(this,
                "¿Mantener logs de cuántos días?",
                "Limpiar Logs Antiguos",
                JOptionPane.QUESTION_MESSAGE);

        if (input != null && !input.trim().isEmpty()) {
            try {
                int dias = Integer.parseInt(input.trim());
                if (dias < 1) {
                    JOptionPane.showMessageDialog(this,
                            "El número de días debe ser mayor a 0",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(this,
                        "¿Está seguro de eliminar logs con más de " + dias + " días?",
                        "Confirmar",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    logService.limpiarLogsAntiguos(dias);
                    setCursor(Cursor.getDefaultCursor());

                    JOptionPane.showMessageDialog(this,
                            "Logs antiguos eliminados correctamente",
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);

                    recargar();
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Ingrese un número válido",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            } catch (ServiceException e) {
                setCursor(Cursor.getDefaultCursor());
                JOptionPane.showMessageDialog(this,
                        "Error al limpiar logs: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    //* MÉTODO PARA ACTUALIZAR DESDE FUERA
    public void recargar() {
        cargarLogs();
    }
}