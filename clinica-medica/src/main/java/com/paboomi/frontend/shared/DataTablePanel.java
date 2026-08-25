package com.paboomi.frontend.shared;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DataTablePanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    public DataTablePanel(String[] columnas) {
        setLayout(new BorderLayout());
        model = new DefaultTableModel(columnas, 0){
            @Override
            public boolean isCellEditable(int row, int column) {return false;}
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void actualizarDatos(List<Object[]> filas){
        model.setRowCount(0);
        for (Object[] row : filas) model.addRow(row);
    }

    public int getFilaSeleccionada () {return table.getSelectedRow();}

    public Object getValorCelda(int fila, int columna) {return model.getValueAt(fila, columna);}
}
