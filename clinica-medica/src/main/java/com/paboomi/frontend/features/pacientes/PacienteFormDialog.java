package com.paboomi.frontend.features.pacientes;

import com.paboomi.backend.models.Paciente;
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
    private JComboBox<String> cmbTipoSangre;

    public PacienteFormDialog(Frame parent) {
        super(parent, "Registrar Nuevo Paciente");
        inicializarCampos();
    }

    private void inicializarCampos() {
        txtIdentificacion = new JTextField();
        txtNombres = new JTextField();
        cmbTipoSangre = new JComboBox<>(new String[]{"A+", "O+", "B+", "AB+"});

        //* Componente reutilizable
        pnlContenido.add(new FormGroupPanel("Número de Identificación Personal:", txtIdentificacion));
        pnlContenido.add(new FormGroupPanel("Nombres y Apellidos:", txtNombres));
        pnlContenido.add(new FormGroupPanel("Tipo de Sangre:", cmbTipoSangre));
    }

    @Override
    protected void onGuardar() {
        //* Llamamos a nuestro facade
        ClinicaFacade clinicaFacade = new ClinicaFacade();

        String id = txtIdentificacion.getText();
        String nombres = txtNombres.getText();
        String tipoSangre = cmbTipoSangre.getSelectedItem().toString();
        Paciente paciente = new Paciente();

        try {
            if (!esValido()){
                clinicaFacade.registrarPaciente(paciente);
            }else{
                System.out.println("Guardando paciente: " + nombres);
                dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private boolean esValido(){
        return !txtIdentificacion.getText().isEmpty() && !txtNombres.getText().isEmpty();
    }
}
