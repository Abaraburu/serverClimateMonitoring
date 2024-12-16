package org.example;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.*;

public class serverStart extends JFrame implements ClimateInterface {

    private JButton startServerButton;
    private JPanel serverStartPanel;
    private JButton stopServerButton;
    private JLabel status;

    // Configurazione del database
    private static final String URL = "jdbc:postgresql://localhost:5432/climatedb";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Asdf1234";

    private Connection conn;
    private boolean isServerRunning = false;

    // Costruttore
    public serverStart() {
        setContentPane(serverStartPanel);
        setTitle("Server Climate Monitoring");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setVisible(true);

        // Listener per il pulsante di avvio del server
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isServerRunning) {
                    try {
                        startRmiServer();
                        JOptionPane.showMessageDialog(serverStart.this, "Server avviato con successo!");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(serverStart.this, "Errore durante l'avvio del server: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(serverStart.this, "Il server è già in esecuzione!");
                }
            }
        });

        // Listener per il pulsante di arresto del server
        stopServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isServerRunning) {
                    isServerRunning = false;
                    status.setText("Status: Server fermo.");
                    JOptionPane.showMessageDialog(serverStart.this, "Server arrestato.");
                    System.exit(0);
                } else {
                    JOptionPane.showMessageDialog(serverStart.this, "Il server è già fermo!");
                }
            }
        });
    }

    // Metodo per avviare il server RMI
    private void startRmiServer() throws RemoteException {
        ClimateInterface stub = (ClimateInterface) UnicastRemoteObject.exportObject(this, 0);
        Registry registry = LocateRegistry.createRegistry(1099);
        registry.rebind("ClimateService", stub);
        isServerRunning = true;

        // Aggiorna lo status della JLabel
        status.setText("Status: Server in esecuzione...");
        System.out.println("Server RMI avviato e registrato.");
    }

    // Metodo per connettersi al database
    private void dbConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connessione al database avvenuta con successo.");
        }
    }

    // Implementazione del metodo remoto per ottenere tutti i dati
    @Override
    public List<Map<String, String>> getAllData() throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>();
        String query = "SELECT * FROM coordinatemonitoraggio";

        try {
            dbConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id_luogo", String.valueOf(rs.getInt("id_luogo")));
                row.put("latitudine", String.valueOf(rs.getDouble("latitudine")));
                row.put("longitudine", String.valueOf(rs.getDouble("longitudine")));
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero dei dati", e);
        }

        return results;
    }

    // Implementazione del metodo remoto per ottenere i dati minimali
    @Override
    public List<Map<String, String>> getMinimalLocationData() throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>();
        String query = "SELECT id_luogo, nome_ascii FROM coordinatemonitoraggio ORDER BY nome_ascii ASC";

        try {
            dbConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id_luogo", String.valueOf(rs.getInt("id_luogo")));
                row.put("nome_ascii", rs.getString("nome_ascii"));
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero dei dati minimali", e);
        }

        return results;
    }

    // Implementazione del metodo remoto per la ricerca per coordinate
    @Override
    public List<Map<String, String>> searchByCoordinates(double latitude, double longitude, double radius) throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>();

        String query = "SELECT id_luogo, latitudine, longitudine, nome_ascii, " +
                "(6371 * acos(cos(radians(?)) * cos(radians(latitudine)) * " +
                "cos(radians(longitudine) - radians(?)) + sin(radians(?)) * sin(radians(latitudine)))) AS distance " +
                "FROM coordinatemonitoraggio " +
                "WHERE (6371 * acos(cos(radians(?)) * cos(radians(latitudine)) * " +
                "cos(radians(longitudine) - radians(?)) + sin(radians(?)) * sin(radians(latitudine)))) <= ? " +
                "ORDER BY distance";

        try {
            dbConnection();
            PreparedStatement stmt = conn.prepareStatement(query);

            // Parametri per il calcolo della distanza
            stmt.setDouble(1, latitude);
            stmt.setDouble(2, longitude);
            stmt.setDouble(3, latitude);

            stmt.setDouble(4, latitude);
            stmt.setDouble(5, longitude);
            stmt.setDouble(6, latitude);
            stmt.setDouble(7, radius);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id_luogo", String.valueOf(rs.getInt("id_luogo")));
                row.put("latitudine", String.valueOf(rs.getDouble("latitudine")));
                row.put("longitudine", String.valueOf(rs.getDouble("longitudine")));
                row.put("nome_ascii", rs.getString("nome_ascii"));
                row.put("distance", String.format("%.2f", rs.getDouble("distance")));
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la ricerca per coordinate con raggio", e);
        }

        return results;
    }

    // Implementazione del metodo remoto per la ricerca per nome
    @Override
    public List<Map<String, String>> searchByName(String name) throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>();
        String query = "SELECT * FROM coordinatemonitoraggio WHERE nome_ascii ILIKE ?";

        try {
            dbConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, "%" + name + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id_luogo", String.valueOf(rs.getInt("id_luogo")));
                row.put("latitudine", String.valueOf(rs.getDouble("latitudine")));
                row.put("longitudine", String.valueOf(rs.getDouble("longitudine")));
                row.put("nome_ascii", rs.getString("nome_ascii"));
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la ricerca per nome", e);
        }

        return results;
    }

    @Override
    public boolean validateCredentials(String userId, String password) throws RemoteException {
        String query = "SELECT * FROM operatoriregistrati WHERE id_operatore = ? AND password = ?";
        try {
            dbConnection();
            PreparedStatement stmt = conn.prepareStatement(query);

            // Converte userId in Integer
            int idOperatore = Integer.parseInt(userId.trim());
            stmt.setInt(1, idOperatore); // Primo parametro come INTEGER
            stmt.setString(2, password); // Secondo parametro come STRING

            ResultSet rs = stmt.executeQuery();
            return rs.next(); // Restituisce true se le credenziali esistono
        } catch (NumberFormatException e) {
            throw new RemoteException("L'ID Operatore deve essere un numero valido.", e);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la validazione delle credenziali.", e);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(serverStart::new);
    }
}