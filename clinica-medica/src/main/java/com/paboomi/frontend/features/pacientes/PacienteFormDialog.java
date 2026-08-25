package com.paboomi.frontend.features.pacientes;

import com.paboomi.backend.dto.RegistrarPacienteDTO;
import com.paboomi.backend.models.Paciente;
import com.paboomi.backend.services.PacienteService;
import com.paboomi.frontend.facade.ClinicaFacade;
import com.paboomi.frontend.shared.BaseFormDialog;
import com.paboomi.frontend.shared.FormGroupPanel;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class PacienteFormDialog extends BaseFormDialog {

    private JTextField txtIdentificacion;
    private JTextField txtNombres;
    private JTextField txtApellidos;
    private JTextField txtFechaNacimiento;
    private JComboBox<String> cmbSexo;
    private JTextField txtTelefono;
    private JTextField txtEmail;
    private JComboBox<String> cmbTipoSangre;

    private PacienteService pacienteService;

    public PacienteFormDialog(Frame parent, PacienteService pacienteService) {
        super(parent, "Registrar Nuevo Paciente");
        this.pacienteService = pacienteService;
        inicializarCampos();
    }

    private void inicializarCampos() {
        txtIdentificacion = new JTextField();
        txtNombres = new JTextField();
        txtApellidos = new JTextField();
        txtFechaNacimiento = new JTextField();
        txtFechaNacimiento.setToolTipText("Formato: yyyy-MM-dd");
        cmbSexo = new JComboBox<>(new String[]{"M", "F"});
        txtTelefono = new JTextField();
        txtEmail = new JTextField();
        cmbTipoSangre = new JComboBox<>(new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}); // Opciones permitidas

        pnlContenido.add(new FormGroupPanel("Identificación (DPI/Pasaporte):", txtIdentificacion));
        pnlContenido.add(new FormGroupPanel("Nombres:", txtNombres));
        pnlContenido.add(new FormGroupPanel("Apellidos:", txtApellidos));
        pnlContenido.add(new FormGroupPanel("Fecha Nacimiento (yyyy-MM-dd):", txtFechaNacimiento));
        pnlContenido.add(new FormGroupPanel("Sexo:", cmbSexo));
        pnlContenido.add(new FormGroupPanel("Teléfono:", txtTelefono));
        pnlContenido.add(new FormGroupPanel("Email (Opcional):", txtEmail));
        pnlContenido.add(new FormGroupPanel("Tipo de Sangre:", cmbTipoSangre));
    }

    @Override
    protected void onGuardar() {
        try {

            RegistrarPacienteDTO dto = new RegistrarPacienteDTO();
            dto.setIdentificacion(txtIdentificacion.getText());
            dto.setNombres(txtNombres.getText());
            dto.setApellidos(txtApellidos.getText());
            dto.setFechaNacimiento(txtFechaNacimiento.getText());
            dto.setSexo(cmbSexo.getSelectedItem().toString());
            dto.setTelefono(txtTelefono.getText());
            dto.setEmail(txtEmail.getText().isEmpty() ? null : txtEmail.getText());
            dto.setTipoSangre(cmbTipoSangre.getSelectedItem().toString());

            pacienteService.registrarPaciente(dto);

            JOptionPane.showMessageDialog(this, "Paciente registrado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();


        } catch (Exception e) {
            //! Se captura la ServiceException para mostrar el mensaje de validación al usuario
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error de Validación", JOptionPane.ERROR_MESSAGE);
        }
    }
}
