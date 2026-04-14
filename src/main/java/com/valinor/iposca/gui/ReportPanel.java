package com.valinor.iposca.gui;

import com.valinor.iposca.dao.ReportDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;

public class ReportPanel extends JPanel {

    private final ReportDAO reportDAO = new ReportDAO();

    private JTextField startDateField;
    private JTextField endDateField;
    private JTextArea outputArea;

    public ReportPanel() {
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        topPanel.add(new JLabel("Start Date (yyyy-mm-dd):"));
        startDateField = new JTextField(10);
        startDateField.setText("2026-04-01");
        topPanel.add(startDateField);

        topPanel.add(new JLabel("End Date (yyyy-mm-dd):"));
        endDateField = new JTextField(10);
        endDateField.setText("2026-04-30");
        topPanel.add(endDateField);

        JButton salesReportButton = new JButton("Sales Report");
        JButton lowStockButton = new JButton("Low Stock Report");
        JButton debtorButton = new JButton("Debtor Report");
        JButton printButton = new JButton("Print");
        JButton saveButton = new JButton("Save");
        System.out.println("Save button added");

        topPanel.add(salesReportButton);
        topPanel.add(lowStockButton);
        topPanel.add(debtorButton);
        topPanel.add(printButton);
        topPanel.add(saveButton);

        add(topPanel, BorderLayout.NORTH);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setLineWrap(false);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        salesReportButton.addActionListener(e -> generateSalesReport());
        lowStockButton.addActionListener(e -> generateLowStockReport());
        debtorButton.addActionListener(e -> generateDebtorReport());
        printButton.addActionListener(e -> printReport());
        saveButton.addActionListener(e -> saveReportToFile());
    }

    private void generateSalesReport() {
        String startDate = startDateField.getText().trim();
        String endDate = endDateField.getText().trim();

        if (startDate.isEmpty() || endDate.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both start and end dates.",
                    "Missing Dates",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        outputArea.setText(reportDAO.generateSalesReport(startDate, endDate));
        outputArea.setCaretPosition(0);
    }

    private void generateLowStockReport() {
        outputArea.setText(reportDAO.generateLowStockReport());
        outputArea.setCaretPosition(0);
    }

    private void generateDebtorReport() {
        outputArea.setText(reportDAO.generateDebtorReport());
        outputArea.setCaretPosition(0);
    }

    private void printReport() {
        try {
            boolean done = outputArea.print();
            if (!done) {
                JOptionPane.showMessageDialog(this,
                        "Printing cancelled. Use 'Save Report' instead.");
            }
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this,
                    "No print service found. Use 'Save Report' instead.",
                    "Print Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void saveReportToFile() {
        if (outputArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No report to save.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Report");
        chooser.setSelectedFile(new java.io.File("report.txt"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".txt")) {
            file = new java.io.File(file.getAbsolutePath() + ".txt");
        }

        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(outputArea.getText());
            JOptionPane.showMessageDialog(this,
                    "Report saved successfully.",
                    "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error saving file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}