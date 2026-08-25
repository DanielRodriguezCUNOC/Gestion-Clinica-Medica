package com.paboomi.frontend.features.citas;

import com.paboomi.frontend.shared.BaseFormDialog;
import com.paboomi.backend.dto.RegistrarCitaDTO;
import com.paboomi.frontend.facade.ClinicaFacade;
import com.paboomi.frontend.shared.FormGroupPanel;

import javax.swing.*;
import java.awt.*;

public class CitaFormDialog extends BaseFormDialog {

    private JTextField txtDpiPaciente;
    private JTextField txtIdMedico;
    private JTextField txtFecha;
    private JTextField txtHoraInicio;
    private JTextArea txtMotivo;
    private JTextArea txtObservaciones;

    public CitaFormDialog(Frame parent) {
        super(parent, "Programar Nueva Cita Médica");
        inicializarCampos();
    }

    private void inicializarCampos() {
        txtDpiPaciente = new JTextField();
        txtIdMedico = new JTextField();
        txtFecha = new JTextField();
        txtFecha.setToolTipText("Formato: yyyy-MM-dd");
        txtHoraInicio = new JTextField();
        txtHoraInicio.setToolTipText("Formato: HH:mm");

        txtMotivo = new JTextArea(3, 20);
        txtMotivo.setLineWrap(true);
        txtObservaciones = new JTextArea(3, 20);
        txtObservaciones.setLineWrap(true);

        pnlContenido.add(new FormGroupPanel("Identificación Paciente (DPI):", txtDpiPaciente));
        pnlContenido.add(new FormGroupPanel("UUID del Médico:", txtIdMedico));
        pnlContenido.add(new FormGroupPanel("Fecha (yyyy-MM-dd):", txtFecha));
        pnlContenido.add(new FormGroupPanel("Hora Inicio (HH:mm):", txtHoraInicio));
        pnlContenido.add(new FormGroupPanel("Motivo de la consulta:", new JScrollPane(txtMotivo)));
        pnlContenido.add(new FormGroupPanel("Observaciones (Opcional):", new JScrollPane(txtObservaciones)));
    }

    @Override
    protected void onGuardar() {
        try {
            RegistrarCitaDTO dto = new RegistrarCitaDTO();
            dto.setIdentificacionPaciente(txtDpiPaciente.getText().trim());
            dto.setIdMedico(txtIdMedico.getText().trim());
            dto.setFecha(txtFecha.getText().trim());
            dto.setHoraInicio(txtHoraInicio.getText().trim());
            dto.setMotivo(txtMotivo.getText().trim());
            dto.setObservaciones(txtObservaciones.getText().trim());

            ClinicaFacade.getInstance().programarCita(dto);
            JOptionPane.showMessageDialog(this, "Cita programada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error al Programar Cita", JOptionPane.ERROR_MESSAGE);
        }
    }
}