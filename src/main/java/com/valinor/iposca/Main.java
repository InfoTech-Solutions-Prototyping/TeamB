package com.valinor.iposca;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.gui.SignInFrame;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;

// Entry point for IPOS-CA.
// Sets up the database, applies the theme, and opens the sign in window.

public class Main {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }

        AppTheme.applyGlobalDefaults();

        // set up database tables
        DatabaseManager.initialiseDatabase();

        try {
            new API.CatalogueServer().start();
        } catch (Exception e) {
            System.err.println("Could not start catalogue server: " + e.getMessage());
        }

        // open the sign in window
        SwingUtilities.invokeLater(() -> {
            SignInFrame signIn = new SignInFrame();
            signIn.setVisible(true);
        });
    }
}
