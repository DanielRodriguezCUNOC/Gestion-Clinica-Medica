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

    // En DataTablePanel.java

    /**
     * Limpia todas las filas de la tabla
     */
    public void limpiar() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
    }

    /**
     * Agrega una fila a la tabla
     */
    public void agregarFila(Object[] fila) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(fila);
    }

    /**
     * Obtiene el número de filas
     */
    public int getRowCount() {
        return table.getRowCount();
    }

    /**
     * Obtiene el número de columnas
     */
    public int getColumnCount() {
        return table.getColumnCount();
    }

    /**
     * Obtiene el valor de una celda
     */
    public Object getValueAt(int row, int col) {
        return table.getValueAt(row, col);
    }

    /**
     * Obtiene los nombres de las columnas
     */
    public String[] getColumnNames() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int columnCount = model.getColumnCount();
        String[] names = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            names[i] = model.getColumnName(i);
        }
        return names;
    }
}
