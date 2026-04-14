package com.valinor.iposca.gui;

import com.valinor.iposca.dao.TemplateDAO;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TemplatePanel extends JPanel {

    private final TemplateDAO dao = new TemplateDAO();

    private JTextField pharmacyNameField;
    private JTextField addressField;
    private JTextField phoneField;
    private JTextField emailField;

    private JTextArea firstReminderArea;
    private JTextArea secondReminderArea;
    private JTextArea invoiceFooterArea;

    public TemplatePanel() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.bg());

        add(AppTheme.headerBar("Template Management"), BorderLayout.NORTH);

        JPanel content = AppTheme.contentPanel();
        content.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.bg());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        pharmacyNameField = new JTextField(30);
        addressField = new JTextField(30);
        phoneField = new JTextField(30);
        emailField = new JTextField(30);

        firstReminderArea = new JTextArea(15, 60);
        secondReminderArea = new JTextArea(15, 60);
        firstReminderArea.setLineWrap(true);
        firstReminderArea.setWrapStyleWord(true);

        secondReminderArea.setLineWrap(true);
        secondReminderArea.setWrapStyleWord(true);

        invoiceFooterArea = new JTextArea(4, 40);

        int y = 0;
        addRow(form, gc, y++, "Pharmacy Name:", pharmacyNameField);
        addRow(form, gc, y++, "Address:", addressField);
        addRow(form, gc, y++, "Phone:", phoneField);
        addRow(form, gc, y++, "Email:", emailField);
        // 1st Reminder
        gc.gridx = 0;
        gc.gridy = y;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel("1st Reminder Template:"), gc);

        gc.gridx = 1;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;

        JScrollPane firstScroll = new JScrollPane(firstReminderArea);
        firstScroll.setPreferredSize(new Dimension(700, 200));
        form.add(firstScroll, gc);

        y++;

        // 2nd Reminder
        gc.gridx = 0;
        gc.gridy = y;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel("2nd Reminder Template:"), gc);

        gc.gridx = 1;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;

        JScrollPane secondScroll = new JScrollPane(secondReminderArea);
        secondScroll.setPreferredSize(new Dimension(700, 200));
        form.add(secondScroll, gc);
        y++;

        addRow(form, gc, y++, "Invoice Footer:", new JScrollPane(invoiceFooterArea));

        content.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        buttons.setBackground(AppTheme.bg());

        JButton loadBtn = AppTheme.btn("Reload");
        loadBtn.addActionListener(e -> loadData());
        buttons.add(loadBtn);

        JButton previewBtn = AppTheme.btn("Preview 1st Reminder");
        previewBtn.addActionListener(e -> previewReminder(firstReminderArea.getText()));
        buttons.add(previewBtn);

        JButton saveBtn = AppTheme.primaryBtn("Save Templates");
        saveBtn.addActionListener(e -> saveData());
        buttons.add(saveBtn);

        content.add(buttons, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);

        loadData();
    }

    private void addRow(JPanel panel, GridBagConstraints gc, int y, String label, Component comp) {
        gc.gridx = 0; gc.gridy = y; gc.weightx = 0;
        panel.add(new JLabel(label), gc);
        gc.gridx = 1; gc.weightx = 1;
        panel.add(comp, gc);
    }

    private void loadData() {
        Map<String, String> d = dao.getAllDetails();
        pharmacyNameField.setText(d.getOrDefault("pharmacy_name", ""));
        addressField.setText(d.getOrDefault("address", ""));
        phoneField.setText(d.getOrDefault("phone", ""));
        emailField.setText(d.getOrDefault("email", ""));
        firstReminderArea.setText(d.getOrDefault("first_reminder_template", ""));
        secondReminderArea.setText(d.getOrDefault("second_reminder_template", ""));
        invoiceFooterArea.setText(d.getOrDefault("invoice_footer", ""));
    }

    private void saveData() {
        boolean ok = true;
        ok &= dao.setDetail("pharmacy_name", pharmacyNameField.getText().trim());
        ok &= dao.setDetail("address", addressField.getText().trim());
        ok &= dao.setDetail("phone", phoneField.getText().trim());
        ok &= dao.setDetail("email", emailField.getText().trim());
        ok &= dao.setDetail("first_reminder_template", firstReminderArea.getText().trim());
        ok &= dao.setDetail("second_reminder_template", secondReminderArea.getText().trim());
        ok &= dao.setDetail("invoice_footer", invoiceFooterArea.getText().trim());

        JOptionPane.showMessageDialog(this,
                ok ? "Templates saved." : "Some values failed to save.",
                "Templates",
                ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    private void previewReminder(String template) {
        String preview = template
                .replace("{customer_name}", "John Smith")
                .replace("{address}", "123 Street")
                .replace("{today}", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))                .replace("{account_id}", "12345")
                .replace("{balance}", "124.50")
                .replace("{due_date}", "23/04/2026")
                .replace("{pharmacy_name}", pharmacyNameField.getText().trim());
        JTextArea ta = new JTextArea(preview);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);

        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(this, sp, "Preview", JOptionPane.INFORMATION_MESSAGE);
    }
}