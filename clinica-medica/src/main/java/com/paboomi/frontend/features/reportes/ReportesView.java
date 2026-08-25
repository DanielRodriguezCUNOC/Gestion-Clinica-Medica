package com.paboomi.frontend.features.reportes;

import com.paboomi.backend.dto.CitaDTO;
import com.paboomi.backend.dto.MedicoDTO;
import com.paboomi.backend.dto.PacienteDTO;
import com.paboomi.backend.services.LogService;
import com.paboomi.backend.models.LogEntry;
import com.paboomi.frontend.facade.ClinicaFacade;
import com.paboomi.frontend.shared.DataTablePanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReportesView extends JPanel {

    private final LogService logService;
    private DataTablePanel tablaReportes;
    private JComboBox<String> cmbTipoReporte;
    private JButton btnGenerar;
    private JButton btnExportar;


    private ReporteLogsView reporteLogsView;
    private JPanel panelLogs;
    private JPanel panelReporteGenerico;
    private CardLayout cardLayoutReportes;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReportesView(LogService logService) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        this.logService = logService;

        initToolbar();
        initContenidoReportes();
    }

    private void initToolbar() {
        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        cmbTipoReporte = new JComboBox<>(new String[]{
                "Resumen de Citas por Estado",
                "Médicos Activos por Especialidad",
                "Pacientes por Tipo de Sangre",
                "Pacientes con Más Citas",
                "Citas por Médico",
                "Citas por Rango de Fechas",
                "Logs de Operaciones y Auditoría"
        });
        btnGenerar = new JButton("Consultar");
        btnExportar = new JButton("Exportar / Imprimir");

        pnlTop.add(new JLabel("Tipo de Reporte:"));
        pnlTop.add(cmbTipoReporte);
        pnlTop.add(btnGenerar);
        pnlTop.add(btnExportar);

        add(pnlTop, BorderLayout.NORTH);

        btnGenerar.addActionListener(e -> consultarDatos());
        btnExportar.addActionListener(e -> exportarDatos());
    }

    private void initContenidoReportes() {

        cardLayoutReportes = new CardLayout();
        JPanel pnlContenedor = new JPanel(cardLayoutReportes);

        panelReporteGenerico = new JPanel(new BorderLayout());
        String[] columnas = {"Registro ID", "Fecha / Hora", "Módulo", "Detalle de Evento", "Usuario"};
        tablaReportes = new DataTablePanel(columnas);
        panelReporteGenerico.add(tablaReportes, BorderLayout.CENTER);

        panelLogs = new JPanel(new BorderLayout());
        try {
            reporteLogsView = new ReporteLogsView(logService);
            panelLogs.add(reporteLogsView, BorderLayout.CENTER);
        } catch (Exception e) {
            JLabel lblError = new JLabel("Error al cargar logs: " + e.getMessage());
            lblError.setForeground(Color.RED);
            panelLogs.add(lblError, BorderLayout.CENTER);
        }

        pnlContenedor.add(panelReporteGenerico, "GENERICO");
        pnlContenedor.add(panelLogs, "LOGS");

        add(pnlContenedor, BorderLayout.CENTER);

        cardLayoutReportes.show(pnlContenedor, "GENERICO");
    }

    private void consultarDatos() {
        String tipoSeleccionado = (String) cmbTipoReporte.getSelectedItem();

        if (tipoSeleccionado.equals("Logs de Operaciones y Auditoría")) {
            cardLayoutReportes.show((JPanel) getComponent(1), "LOGS");
            if (reporteLogsView != null) {
                reporteLogsView.recargar();
            }
            btnExportar.setEnabled(false);
        } else {
            cardLayoutReportes.show((JPanel) getComponent(1), "GENERICO");
            btnExportar.setEnabled(true);

            switch (tipoSeleccionado) {
                case "Resumen de Citas por Estado":
                    //cargarReporteCitasPorEstado();
                    break;
                case "Médicos Activos por Especialidad":
                    cargarReporteMedicosPorEspecialidad();
                    break;
                case "Pacientes por Tipo de Sangre":
                    cargarReportePacientesPorTipoSangre();
                    break;
                case "Pacientes con Más Citas":
                    cargarReportePacientesConMasCitas();
                    break;
                case "Citas por Médico":
                    cargarReporteCitasPorMedico();
                    break;
                case "Citas por Rango de Fechas":
                    //cargarReporteCitasPorRangoFechas();
                    break;
                default:
                    JOptionPane.showMessageDialog(this, "Reporte no implementado", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    

    private void cargarReporteMedicosPorEspecialidad() {
        try {
            //* Obtener todos los médicos
            List<MedicoDTO> medicos = ClinicaFacade.getInstance().listarMedicos();

            //* Agrupar por especialidad
            Map<String, Long> conteoPorEspecialidad = medicos.stream()
                    .filter(m -> m.getEstado().equals("Activo"))
                    .collect(Collectors.groupingBy(
                            MedicoDTO::getEspecialidad,
                            Collectors.counting()
                    ));

            //* Limpiar tabla y mostrar resultados
            tablaReportes.limpiar();

            //* Título del reporte
            tablaReportes.agregarFila(new Object[]{
                    "--",
                    LocalDateTime.now().format(DATE_FORMATTER),
                    "Médicos Activos por Especialidad",
                    "Total de médicos activos: " + medicos.stream().filter(m -> m.getEstado().equals("Activo")).count(),
                    "Sistema"
            });

            //* Agregar fila en blanco como separador
            tablaReportes.agregarFila(new Object[]{"", "", "", "", ""});

            //* Ordenar especialidades alfabéticamente
            int contador = 1;
            for (Map.Entry<String, Long> entry : conteoPorEspecialidad.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList())) {

                tablaReportes.agregarFila(new Object[]{
                        contador++,
                        LocalDateTime.now().format(DATE_FORMATTER),
                        entry.getKey(),
                        "Médicos: " + entry.getValue(),
                        "Sistema"
                });
            }

            //* Calcular porcentaje de médicos por especialidad
            long totalActivos = medicos.stream().filter(m -> m.getEstado().equals("Activo")).count();
            if (totalActivos > 0) {

                //! Separador
                tablaReportes.agregarFila(new Object[]{"", "", "", "", ""});
                tablaReportes.agregarFila(new Object[]{
                        "--",
                        LocalDateTime.now().format(DATE_FORMATTER),
                        "Porcentajes",
                        "",
                        "Sistema"
                });

                for (Map.Entry<String, Long> entry : conteoPorEspecialidad.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .collect(Collectors.toList())) {

                    double porcentaje = (entry.getValue() * 100.0) / totalActivos;
                    tablaReportes.agregarFila(new Object[]{
                            "",
                            "",
                            entry.getKey(),
                            String.format("%.1f%% del total", porcentaje),
                            "Sistema"
                    });
                }
            }

            //* Registrar en log
            logService.registrarLog(
                    LogEntry.Modulo.REPORTES,
                    LogEntry.Accion.CONSULTA,
                    "Reporte de Médicos por Especialidad generado - Especialidades: " + conteoPorEspecialidad.size(),
                    null
            );

            JOptionPane.showMessageDialog(this,
                    "Reporte de Médicos por Especialidad generado exitosamente",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al generar reporte: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarReportePacientesPorTipoSangre() {
        try {
            List<PacienteDTO> pacientes = ClinicaFacade.getInstance().listarPacientes();

            Map<String, Long> conteoPorTipoSangre = pacientes.stream()
                    .collect(Collectors.groupingBy(
                            PacienteDTO::getTipoSangre,
                            Collectors.counting()
                    ));

            tablaReportes.limpiar();

            int contador = 1;
            for (Map.Entry<String, Long> entry : conteoPorTipoSangre.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .toList()) {

                tablaReportes.agregarFila(new Object[]{
                        contador++,
                        LocalDateTime.now().format(DATE_FORMATTER),
                        "Pacientes - Tipo Sangre",
                        "Tipo " + entry.getKey() + ": " + entry.getValue() + " pacientes",
                        "Sistema"
                });
            }

            logService.registrarLog(
                    LogEntry.Modulo.REPORTES,
                    LogEntry.Accion.CONSULTA,
                    "Reporte de Pacientes por Tipo de Sangre generado",
                    null
            );

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al generar reporte: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarReporteCitasPorMedico() {
        try {
            List<MedicoDTO> medicos = ClinicaFacade.getInstance().listarMedicos();

            tablaReportes.limpiar();

            int contador = 1;
            for (MedicoDTO medico : medicos) {
                // Obtener citas del médico
                List<CitaDTO> citasMedico = ClinicaFacade.getInstance().buscarCitasPorMedico(UUID.fromString(medico.getId()));
                long totalCitas = citasMedico.size();
                long programadas = citasMedico.stream().filter(c -> c.getEstado().equals("Programada")).count();

                tablaReportes.agregarFila(new Object[]{
                        contador++,
                        LocalDateTime.now().format(DATE_FORMATTER),
                        medico.getNombresCompletos(),
                        "Especialidad: " + medico.getEspecialidad() + " | Citas: " + totalCitas + " (Programadas: " + programadas + ")",
                        "Sistema"
                });
            }

            logService.registrarLog(
                    LogEntry.Modulo.REPORTES,
                    LogEntry.Accion.CONSULTA,
                    "Reporte de Citas por Médico generado",
                    null
            );

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al generar reporte: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarReportePacientesConMasCitas() {
        try {
            List<PacienteDTO> pacientesConMasCitas = ClinicaFacade.getInstance().obtenerPacientesConMasCitas();

            tablaReportes.limpiar();

            if (pacientesConMasCitas.isEmpty()) {
                tablaReportes.agregarFila(new Object[]{
                        "--",
                        LocalDateTime.now().format(DATE_FORMATTER),
                        "Pacientes - Más Citas",
                        "No hay pacientes con citas registradas",
                        "Sistema"
                });
            } else {
                tablaReportes.agregarFila(new Object[]{
                        "--",
                        LocalDateTime.now().format(DATE_FORMATTER),
                        "Pacientes - Más Citas",
                        "Pacientes con mayor cantidad de citas:",
                        "Sistema"
                });

                int contador = 1;
                for (PacienteDTO paciente : pacientesConMasCitas) {
                    tablaReportes.agregarFila(new Object[]{
                            contador++,
                            LocalDateTime.now().format(DATE_FORMATTER),
                            paciente.getNombresCompletos(),
                            "Identificación: " + paciente.getIdentificacion(),
                            "Sistema"
                    });
                }
            }

            logService.registrarLog(
                    LogEntry.Modulo.REPORTES,
                    LogEntry.Accion.CONSULTA,
                    "Reporte de Pacientes con Más Citas generado",
                    null
            );

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al generar reporte: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportarDatos() {
        // Solo exportar si estamos en el panel genérico
        if (tablaReportes == null || tablaReportes.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No hay datos para exportar. Primero genere un reporte.",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar Reporte como CSV");
        fc.setSelectedFile(new File("reporte_clinica.csv"));

        int userSelection = fc.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();

            if (!archivo.getName().toLowerCase().endsWith(".csv")) {
                archivo = new File(archivo.getAbsolutePath() + ".csv");
            }

            try (PrintWriter out = new PrintWriter(new FileWriter(archivo))) {
                // Escribir cabecera
                StringBuilder sb = new StringBuilder();
                String[] columnas = tablaReportes.getColumnNames();
                for (int i = 0; i < columnas.length; i++) {
                    sb.append(columnas[i]);
                    if (i < columnas.length - 1) sb.append(";");
                }
                sb.append("\n");
                out.write(sb.toString());

                // Escribir datos
                for (int row = 0; row < tablaReportes.getRowCount(); row++) {
                    sb = new StringBuilder();
                    for (int col = 0; col < tablaReportes.getColumnCount(); col++) {
                        Object valor = tablaReportes.getValueAt(row, col);
                        sb.append(valor != null ? valor.toString().replace(";", ",") : "");
                        if (col < tablaReportes.getColumnCount() - 1) sb.append(";");
                    }
                    sb.append("\n");
                    out.write(sb.toString());
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Reporte exportado exitosamente en:\n" + archivo.getAbsolutePath(),
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // Registrar en log
                logService.registrarLog(
                        LogEntry.Modulo.REPORTES,
                        LogEntry.Accion.EXPORTACIÓN,
                        "Reporte exportado: " + cmbTipoReporte.getSelectedItem(),
                        null
                );

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error al guardar el archivo: " + e.getMessage(),
                        "Error de exportación",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Recarga los datos (útil cuando se cambia a esta vista)
     */
    public void recargar() {
        if (reporteLogsView != null) {
            reporteLogsView.recargar();
        }
    }
}