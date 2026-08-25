package com.paboomi.frontend.shared;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class FormGroupPanel extends JPanel {

    private JLabel lblTitulo;
    private final JComponent inputComponent;

    public FormGroupPanel(String lblTitulo, JComponent inputComponent) {
        this.inputComponent = inputComponent;
        setupComponent(lblTitulo);
    }

    public void setupComponent(String titulo){

        setLayout(new BorderLayout());
        lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD));

        add(lblTitulo, BorderLayout.NORTH);
        add(this.inputComponent, BorderLayout.CENTER);

        setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
    }
}
