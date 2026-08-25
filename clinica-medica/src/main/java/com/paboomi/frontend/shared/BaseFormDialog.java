package com.paboomi.frontend.shared;

import javax.swing.*;
import java.awt.*;

public abstract class BaseFormDialog extends JDialog {

    protected JPanel pnlContenido;
    protected JButton btnGuardar;
    protected JButton btnCancelar;

    public BaseFormDialog(Frame parent, String titulo) {
        //* true = modal, bloquea la ventana de atrás
        super(parent, titulo, true);
        setSize(400, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        pnlContenido = new JPanel();
        pnlContenido.setLayout(new BoxLayout(pnlContenido, BoxLayout.Y_AXIS));
        pnlContenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(new JScrollPane(pnlContenido), BorderLayout.CENTER);

        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnGuardar = new JButton("Guardar");
        btnCancelar = new JButton("Cancelar");

        pnlBotones.add(btnCancelar);
        pnlBotones.add(btnGuardar);

        add(pnlBotones, BorderLayout.SOUTH);

        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> onGuardar());
    }

    //* Para la lógica de cada formulario
    protected abstract void onGuardar();
}