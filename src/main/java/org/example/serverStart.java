package org.example;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class serverStart extends JFrame {

    private JButton startServerButton;
    private JPanel serverStartPanel;

    // Configurazione del database
    private static final String URL = "jdbc:postgresql://localhost:5432/serverClimateMonitoring";
    private static final String USER = "climate_user";
    private static final String PASSWORD = "secure_password";

    public serverStart() {
        setContentPane(serverStartPanel);
        setTitle("Server Climate Monitoring");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setVisible(true);

        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Avvio server...");
                connectToDatabase();
            }
        });
    }

    // Metodo per connettersi al database
    private void connectToDatabase() {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            JOptionPane.showMessageDialog(this, "Server avviato senza problemi");
            System.out.println("Connected to the database!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Errore: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new serverStart();
    }
}
