package com.valinor.iposca.gui;

import com.valinor.iposca.dao.UserDAO;
import com.valinor.iposca.model.ApplicationUser;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Admin panel for managing user accounts.
 * Create users, change roles, remove users, search.
 */
public class UserPanel extends JPanel {

    private UserDAO userDAO;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    // password column hidden for security
    private final String[] columns = {"User ID", "Username", "Role", "Created"};

    public UserPanel() {
        userDAO = new UserDAO();
        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.bg());

        add(AppTheme.headerBar("User Management"), BorderLayout.NORTH);

        JPanel content = AppTheme.contentPanel();

        // search bar
        Object[] sb = AppTheme.searchBar(this::performSearch, this::refreshTable);
        content.add((JPanel) sb[0], BorderLayout.NORTH);
        searchField = (JTextField) sb[1];

        // table
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        AppTheme.styleTable(userTable);

        JScrollPane sp = new JScrollPane(userTable);
        AppTheme.styleScrollPane(sp);
        content.add(sp, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        // buttons
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        bar.setBackground(AppTheme.bg());
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JButton addBtn = AppTheme.btn("Add User");
        addBtn.addActionListener(e -> showAddUserDialog());
        bar.add(addBtn);

        JButton roleBtn = AppTheme.btn("Change Role");
        roleBtn.addActionListener(e -> changeSelectedUserRole());
        bar.add(roleBtn);

        JButton delBtn = AppTheme.btn("Delete User");
        delBtn.addActionListener(e -> deleteSelectedUser());
        bar.add(delBtn);

        JButton refBtn = AppTheme.btn("Refresh");
        refBtn.addActionListener(e -> refreshTable());
        bar.add(refBtn);

        add(bar, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<ApplicationUser> users = userDAO.getAllUsers();
        for (ApplicationUser u : users) {
            tableModel.addRow(new Object[]{
                    u.getUserID(), u.getUsername(), u.getRole(), u.getCreatedAt()
            });
        }
    }

    private void performSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { refreshTable(); return; }

        tableModel.setRowCount(0);
        List<ApplicationUser> users = userDAO.searchUsers(kw);
        for (ApplicationUser u : users) {
            tableModel.addRow(new Object[]{
                    u.getUserID(), u.getUsername(), u.getRole(), u.getCreatedAt()
            });
        }
        if (users.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No users found matching: " + kw);
        }
    }

    private void showAddUserDialog() {
        JTextField userF = new JTextField(15);
        JPasswordField passF = new JPasswordField(15);
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"Pharmacist", "Manager", "Admin"});

        JPanel f = new JPanel(new GridLayout(3, 2, 5, 5));
        f.add(new JLabel("Username:*")); f.add(userF);
        f.add(new JLabel("Password:*")); f.add(passF);
        f.add(new JLabel("Role:"));      f.add(roleBox);

        if (JOptionPane.showConfirmDialog(this, f, "Add New User",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String username = userF.getText().trim();
            String password = new String(passF.getPassword());
            String role = (String) roleBox.getSelectedItem();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password are required.");
                return;
            }

            ApplicationUser user = new ApplicationUser();
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(role);

            int result = userDAO.createUser(user);
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "User '" + username + "' created as " + role + ".");
                refreshTable();
            } else if (result == -1) {
                JOptionPane.showMessageDialog(this, "Username already taken.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create user.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void changeSelectedUserRole() {
        int r = userTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a user first."); return; }

        int userId = (int) tableModel.getValueAt(r, 0);
        String username = (String) tableModel.getValueAt(r, 1);
        String currentRole = (String) tableModel.getValueAt(r, 2);

        String[] roles = {"Pharmacist", "Manager", "Admin"};
        String newRole = (String) JOptionPane.showInputDialog(this,
                "Change role for '" + username + "'\nCurrent: " + currentRole,
                "Change Role", JOptionPane.PLAIN_MESSAGE, null, roles, currentRole);

        if (newRole != null && !newRole.equals(currentRole)) {
            if (userDAO.changeRole(userId, newRole)) {
                JOptionPane.showMessageDialog(this, username + " is now a " + newRole + ".");
                refreshTable();
            }
        }
    }

    private void deleteSelectedUser() {
        int r = userTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a user to delete."); return; }

        int userId = (int) tableModel.getValueAt(r, 0);
        String username = (String) tableModel.getValueAt(r, 1);
        String role = (String) tableModel.getValueAt(r, 2);

        // don't let them delete the last admin
        if ("Admin".equals(role)) {
            long adminCount = 0;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if ("Admin".equals(tableModel.getValueAt(i, 2))) adminCount++;
            }
            if (adminCount <= 1) {
                JOptionPane.showMessageDialog(this, "Cannot delete the last Admin account.",
                        "Protected", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (JOptionPane.showConfirmDialog(this, "Delete user '" + username + "'?",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            if (userDAO.deleteUser(userId)) {
                JOptionPane.showMessageDialog(this, "User deleted.");
                refreshTable();
            }
        }
    }
}