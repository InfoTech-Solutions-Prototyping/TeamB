package com.valinor.iposca;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.gui.SignInFrame;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;

/**
 * Entry point for IPOS-CA.
 * Sets up the database, applies the theme, and opens the sign in window.
 */

public class Main {

    public static void main(String[] args) {

        // cross-platform look and feel gives us full control over colours
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }

        // apply theme colours to all swing components
        AppTheme.applyGlobalDefaults();

        // set up database tables (skips if they already exist)
        DatabaseManager.initialiseDatabase();

        // open the sign in window
        SwingUtilities.invokeLater(() -> {
            SignInFrame signIn = new SignInFrame();
            signIn.setVisible(true);
        });
    }
}
