package com.paboomi.frontend.features.medicos;

import com.paboomi.backend.dto.RegistrarMedicoDTO;
import com.paboomi.backend.services.MedicoService;
import com.paboomi.frontend.shared.BaseFormDialog;
import com.paboomi.frontend.shared.FormGroupPanel;

import javax.swing.*;
import java.awt.*;

public class MedicoFormDialog extends BaseFormDialog {

    private JTextField txtNombres;
    private JTextField txtApellidos;
    private JTextField txtEspecialidad;
    private JTextField txtTelefono;
    private JTextField txtCorreo;
    private JTextField txtHorarioInicio;
    private JTextField txtHorarioFin;

    private MedicoService medicoService;

    public MedicoFormDialog(Frame parent, MedicoService medicoService) {
        super(parent, "Registrar Nuevo Médico");
        this.medicoService = medicoService;
        inicializarCampos();
    }

    private void inicializarCampos() {
        txtNombres = new JTextField();
        txtApellidos = new JTextField();
        txtEspecialidad = new JTextField();
        txtTelefono = new JTextField();
        txtCorreo = new JTextField();
        txtHorarioInicio = new JTextField();
        txtHorarioInicio.setToolTipText("Formato: HH:mm");
        txtHorarioFin = new JTextField();
        txtHorarioFin.setToolTipText("Formato: HH:mm");

        pnlContenido.add(new FormGroupPanel("Nombres:", txtNombres));
        pnlContenido.add(new FormGroupPanel("Apellidos:", txtApellidos));
        pnlContenido.add(new FormGroupPanel("Especialidad:", txtEspecialidad));
        pnlContenido.add(new FormGroupPanel("Teléfono:", txtTelefono));
        pnlContenido.add(new FormGroupPanel("Correo Electrónico (Opcional):", txtCorreo));
        pnlContenido.add(new FormGroupPanel("Horario Inicio (HH:mm):", txtHorarioInicio)); // Formato esperado en UI[cite: 2]
        pnlContenido.add(new FormGroupPanel("Horario Fin (HH:mm):", txtHorarioFin)); // Formato esperado en UI[cite: 2]
    }

    @Override
    protected void onGuardar() {
        try {
            RegistrarMedicoDTO dto = new RegistrarMedicoDTO();
            dto.setNombres(txtNombres.getText());
            dto.setApellidos(txtApellidos.getText());
            dto.setEspecialidad(txtEspecialidad.getText());
            dto.setTelefono(txtTelefono.getText());
            dto.setCorreo(txtCorreo.getText().isEmpty() ? null : txtCorreo.getText());
            dto.setHorarioInicio(txtHorarioInicio.getText());
            dto.setHorarioFin(txtHorarioFin.getText());

            medicoService.registrarMedico(dto);

            JOptionPane.showMessageDialog(this, "Médico registrado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (Exception e) {
            //! Mostrar los mensajes de ServiceException
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error de Validación", JOptionPane.ERROR_MESSAGE);
        }
    }
}
