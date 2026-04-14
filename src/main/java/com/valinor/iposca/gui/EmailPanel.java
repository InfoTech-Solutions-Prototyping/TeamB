package com.valinor.iposca.gui;

import com.valinor.iposca.util.AppTheme;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Properties;

public class EmailPanel extends JPanel {

    // email details
    private static final String EMAIL    = "ipos.ca.smtp@gmail.com";
    private static final String APP_PASS = "thex thtx vwng jmnf";

    private final DefaultTableModel model;
    private final JTextArea bodyArea;

    public EmailPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.bg());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // top bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(AppTheme.bg());

        JButton refreshBtn = AppTheme.primaryBtn("Refresh Inbox");
        refreshBtn.addActionListener(e -> fetchEmails());
        topBar.add(refreshBtn);

        JLabel info = new JLabel("Order emails from PU");
        info.setForeground(AppTheme.text());
        topBar.add(info);

        add(topBar, BorderLayout.NORTH);

        // Email table
        String[] columns = {"Date", "From", "Subject"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        AppTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(400);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(0, 250));

        // Email body
        bodyArea = new JTextArea();
        bodyArea.setEditable(false);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyArea.setBackground(AppTheme.surface());
        bodyArea.setForeground(AppTheme.text());
        bodyArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        bodyArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane bodyScroll = new JScrollPane(bodyArea);

        // When a row is clicked, show the body
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < emailBodies.length) {
                    bodyArea.setText(emailBodies[row]);
                    bodyArea.setCaretPosition(0);
                }
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, bodyScroll);
        split.setDividerLocation(250);
        add(split, BorderLayout.CENTER);
    }

    private String[] emailBodies = new String[0];

    private void fetchEmails() {
        model.setRowCount(0);
        bodyArea.setText("");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private String[][] rows;
            private String[] bodies;
            private String error;

            @Override
            protected Void doInBackground() {
                try {
                    Properties props = new Properties();
                    props.put("mail.store.protocol", "imaps");
                    props.put("mail.imaps.host", "imap.gmail.com");
                    props.put("mail.imaps.port", "993");
                    props.put("mail.imaps.ssl.enable", "true");

                    Session session = Session.getInstance(props);
                    Store store = session.getStore("imaps");
                    store.connect("imap.gmail.com", EMAIL, APP_PASS);

                    Folder inbox = store.getFolder("INBOX");
                    inbox.open(Folder.READ_ONLY);

                    int count = inbox.getMessageCount();
                    System.out.println("IMAP connected. Message count: " + count);

                    if (count == 0) {
                        inbox.close(false);
                        store.close();
                        rows = new String[0][3];
                        bodies = new String[0];
                        return null;
                    }

                    int start = Math.max(1, count - 49);
                    System.out.println("Fetching messages " + start + " to " + count);
                    Message[] messages = inbox.getMessages(start, count);
                    System.out.println("getMessages done: " + messages.length);

                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.ENVELOPE);
                    fp.add(FetchProfile.Item.CONTENT_INFO);
                    inbox.fetch(messages, fp);
                    System.out.println("fetch profile done");


                    System.out.println("Messages fetched: " + messages.length);

                    rows = new String[messages.length][3];
                    bodies = new String[messages.length];
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                    for (int i = 0; i < messages.length; i++) {
                        Message msg = messages[messages.length - 1 - i];
                        try {
                            String date = msg.getSentDate() != null
                                    ? sdf.format(msg.getSentDate()) : "Unknown";
                            String from = msg.getFrom() != null && msg.getFrom().length > 0
                                    ? msg.getFrom()[0].toString() : "Unknown";
                            String subject = msg.getSubject() != null
                                    ? msg.getSubject() : "(No subject)";

                            rows[i] = new String[]{date, from, subject};
                            bodies[i] = getTextFromMessage(msg);
                            System.out.println("Read email: " + subject);
                        } catch (Exception ex) {
                            rows[i] = new String[]{"?", "?", "Error reading email"};
                            bodies[i] = ex.getMessage();
                        }
                    }

                    inbox.close(false);
                    store.close();
                } catch (Throwable ex) {
                    System.err.println("EMAIL ERROR: " + ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace();
                    error = ex.getClass().getName() + ": " + ex.getMessage();
                }

                return null;
            }


            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(EmailPanel.this,
                            "Failed to fetch emails:\n" + error,
                            "Email Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (rows == null || rows.length == 0) {
                    JOptionPane.showMessageDialog(EmailPanel.this,
                            "No emails found in inbox.",
                            "Inbox Empty", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                emailBodies = bodies;
                System.out.println("Adding " + rows.length + " rows to table");
                for (String[] row : rows) {
                    model.addRow(row);
                }
            }

        };
        worker.execute();
    }

    private String getTextFromMessage(Message message) {
        try {
            if (message.isMimeType("text/plain")) {
                return message.getContent().toString();
            } else if (message.isMimeType("text/html")) {
                return message.getContent().toString()
                        .replaceAll("<[^>]+>", "");
            } else if (message.isMimeType("multipart/*")) {
                MimeMultipart mp = (MimeMultipart) message.getContent();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart bp = mp.getBodyPart(i);
                    if (bp.isMimeType("text/plain")) {
                        sb.append(bp.getContent().toString());
                    }
                }
                return sb.length() > 0 ? sb.toString() : "(No text content)";
            }
        } catch (Exception e) {
            return "(Could not read email body: " + e.getMessage() + ")";
        }
        return "(Unsupported email format)";
    }
}
