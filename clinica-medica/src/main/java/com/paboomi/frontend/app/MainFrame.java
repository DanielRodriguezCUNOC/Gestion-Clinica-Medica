package com.paboomi.frontend.app;

import com.paboomi.frontend.facade.ClinicaFacade;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public static final String CARD_PACIENTES = "PACIENTES";
    public static final String CARD_MEDICOS = "MEDICOS";
    public static final String CARD_CITAS = "CITAS";
    public static final String CARD_REPORTES = "REPORTES";

    private JPanel pnlContenidoCentral;
    private CardLayout cardLayout;

    private JPanel pnlPacientes;
    private JPanel pnlMedicos;
    private JPanel pnlCitas;
    private JPanel pnlReportes;

    public MainFrame() {
        //* Configuración básica de la ventana principal
        setTitle("Sistema de Gestión - Clínica Médica");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null); // Centrar en pantalla
        setLayout(new BorderLayout());

        //* Instanciar la fachada principal de la aplicación
        ClinicaFacade.getInstance();

        //* Construir la interfaz
        initHeader();
        initSidebar();
        initCentralArea();
    }

    /**
     * Encabezado superior de la aplicación
     */
    private void initHeader() {
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblTitulo = new JLabel("Clínica Médica");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 20f));

        JLabel lblUsuario = new JLabel("Sesión Activa: Administrador");
        lblUsuario.setFont(lblUsuario.getFont().deriveFont(Font.ITALIC, 12f));

        pnlHeader.add(lblTitulo, BorderLayout.WEST);
        pnlHeader.add(lblUsuario, BorderLayout.EAST);

        add(pnlHeader, BorderLayout.NORTH);
    }

    /**
     * Menú lateral izquierdo para navegación
     */
    private void initSidebar() {
        JPanel pnlSidebar = new JPanel();
        pnlSidebar.setLayout(new BoxLayout(pnlSidebar, BoxLayout.Y_AXIS));
        pnlSidebar.setPreferredSize(new Dimension(200, 0));
        pnlSidebar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //* Botones de navegación para los módulos requeridos
        JButton btnPacientes = crearBotonNavegacion("Pacientes");
        JButton btnMedicos = crearBotonNavegacion("Médicos");
        JButton btnCitas = crearBotonNavegacion("Citas Médicas");
        JButton btnReportes = crearBotonNavegacion("Reportes y Logs");

        //* Asignar eventos para cambiar de vista en el CardLayout
        btnPacientes.addActionListener(e -> mostrarTarjeta(CARD_PACIENTES));
        btnMedicos.addActionListener(e -> mostrarTarjeta(CARD_MEDICOS));
        btnCitas.addActionListener(e -> mostrarTarjeta(CARD_CITAS));
        btnReportes.addActionListener(e -> mostrarTarjeta(CARD_REPORTES));

        //* Agregar botones al panel con espacio vertical
        pnlSidebar.add(btnPacientes);
        pnlSidebar.add(Box.createVerticalStrut(8));
        pnlSidebar.add(btnMedicos);
        pnlSidebar.add(Box.createVerticalStrut(8));
        pnlSidebar.add(btnCitas);
        pnlSidebar.add(Box.createVerticalStrut(8));
        pnlSidebar.add(btnReportes);
        pnlSidebar.add(Box.createVerticalGlue());

        add(pnlSidebar, BorderLayout.WEST);
    }

    /**
     * Área central dinámica donde se intercambiarán las vistas
     */
    private void initCentralArea() {
        cardLayout = new CardLayout();
        pnlContenidoCentral = new JPanel(cardLayout);
        pnlContenidoCentral.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //* Paneles temporales (Placeholders) hasta integrar PacienteView, MedicoView, etc.
        pnlPacientes = new JPanel(new BorderLayout());
        pnlMedicos = new JPanel(new BorderLayout());
        pnlCitas = new JPanel(new BorderLayout());
        pnlReportes = new JPanel(new BorderLayout());

        //* Registrar paneles en el CardLayout
        pnlContenidoCentral.add(pnlPacientes, CARD_PACIENTES);
        pnlContenidoCentral.add(pnlMedicos, CARD_MEDICOS);
        pnlContenidoCentral.add(pnlCitas, CARD_CITAS);
        pnlContenidoCentral.add(pnlReportes, CARD_REPORTES);

        add(pnlContenidoCentral, BorderLayout.CENTER);

        //* Mostrar Pacientes por defecto al iniciar
        mostrarTarjeta(CARD_PACIENTES);
    }

    /**
     * Cambia la tarjeta visible en el CardLayout
     */
    public void mostrarTarjeta(String nombreTarjeta) {
        cardLayout.show(pnlContenidoCentral, nombreTarjeta);
    }

    /**
     * Permite reemplazar dinámicamente el panel de un módulo cuando crees sus vistas (ej. PacienteView)
     */
    public void setModuloPanel(String nombreTarjeta, JPanel nuevoPanel) {
        pnlContenidoCentral.add(nuevoPanel, nombreTarjeta);
        cardLayout.show(pnlContenidoCentral, nombreTarjeta);
    }

    /**
     * Fábrica de botones para mantener coherencia visual en el menú lateral
     */
    private JButton crearBotonNavegacion(String texto) {
        JButton btn = new JButton(texto);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusable(false);
        return btn;
    }
}