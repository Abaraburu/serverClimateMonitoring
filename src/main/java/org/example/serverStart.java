package org.example;

import com.formdev.flatlaf.FlatDarkLaf;
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
    private JTextField textfieldIPandPORT;

    // Configurazione del database
    private static final String USER = "postgres";
    private static final String PASSWORD = "Asdf1234";

    private Connection conn;
    private boolean isServerRunning = false;

    public serverStart() {
        setContentPane(serverStartPanel);
        setTitle("Server Climate Monitoring");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setVisible(true);

        textfieldIPandPORT.setText("localhost:5432");

        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isServerRunning) {
                    String ipAndPort = textfieldIPandPORT.getText().trim();
                    if (ipAndPort.isEmpty() || !ipAndPort.matches("^\\S+:\\d+$")) {
                        JOptionPane.showMessageDialog(serverStart.this, "Inserire un valore valido nel formato IP:PORT.", "Errore", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try {
                        System.out.println("Valore inserito nella textfield: " + ipAndPort);
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

    private void startRmiServer() throws RemoteException {
        ClimateInterface stub = (ClimateInterface) UnicastRemoteObject.exportObject(this, 0);
        Registry registry = LocateRegistry.createRegistry(1099);
        registry.rebind("ClimateService", stub);
        isServerRunning = true;
        status.setText("Status: Server in esecuzione...");
        System.out.println("Server RMI avviato e registrato.");
    }

    private String getDatabaseUrl() {
        String ipAndPort = textfieldIPandPORT.getText().trim();
        return "jdbc:postgresql://" + ipAndPort + "/climatedb";
    }

    private Connection dbConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            String url = getDatabaseUrl();
            conn = DriverManager.getConnection(url, USER, PASSWORD);
            System.out.println("Connessione al database avvenuta con successo.");
        }
        return conn;
    }

    @Override
    public List<Map<String, String>> getAllData() throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>();
        String query = "SELECT id_luogo, nome_ascii FROM coordinatemonitoraggio ORDER BY nome_ascii ASC";

        try (Connection connection = dbConnection(); Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id_luogo", String.valueOf(rs.getInt("id_luogo")));
                row.put("nome_ascii", rs.getString("nome_ascii"));
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero dei dati", e);
        }

        return results;
    }

    @Override
    public List<Map<String, String>> getMinimalLocationData() throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>();
        String query = "SELECT id_luogo, nome_ascii FROM coordinatemonitoraggio ORDER BY nome_ascii ASC";

        try (Connection connection = dbConnection(); Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
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

    @Override
    public boolean registerMonitoringCenter(String name, String address, List<Integer> areaIds) throws RemoteException {
        String insertCenterQuery = "INSERT INTO centrimonitoraggio (nome, indirizzo) VALUES (?, ?) RETURNING id_centromonitoraggio";
        String insertAssociationQuery = "INSERT INTO centroarea (id_centromonitoraggio, id_luogo) VALUES (?, ?)";

        try (Connection connection = dbConnection()) {
            connection.setAutoCommit(false); // Avvia una transazione

            int centerId;
            try (PreparedStatement centerStmt = connection.prepareStatement(insertCenterQuery)) {
                centerStmt.setString(1, name);
                centerStmt.setString(2, address);
                ResultSet rs = centerStmt.executeQuery();
                if (rs.next()) {
                    centerId = rs.getInt("id_centromonitoraggio");
                } else {
                    connection.rollback();
                    throw new SQLException("Errore nel recuperare l'ID del centro di monitoraggio appena inserito.");
                }
            }

            // Inserisce la relazione tra il centro e le aree
            try (PreparedStatement assocStmt = connection.prepareStatement(insertAssociationQuery)) {
                for (int areaId : areaIds) {
                    assocStmt.setInt(1, centerId);
                    assocStmt.setInt(2, areaId);
                    assocStmt.executeUpdate();
                }
            }

            connection.commit(); // Concludi la transazione
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Errore durante la registrazione del centro di monitoraggio", e);
        }
    }

    @Override
    public boolean checkDuplicateMonitoringCenter(String name, String address) throws RemoteException {
        String query = "SELECT COUNT(*) FROM centrimonitoraggio WHERE nome = ? OR indirizzo = ?";
        try (Connection connection = dbConnection(); PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, address);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Restituisce true se esiste un duplicato
            }
        } catch (SQLException e) {
            System.err.println("Errore SQL: " + e.getMessage());
            e.printStackTrace(); // Log dettagliato sul server
            throw new RemoteException("Errore generico durante il controllo dei duplicati");
        }
        return false;
    }

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

        try (PreparedStatement stmt = dbConnection().prepareStatement(query)) {

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

    @Override
    public List<Map<String, String>> searchByName(String name) throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>();
        String query = "SELECT * FROM coordinatemonitoraggio WHERE nome_ascii ILIKE ?";

        try (PreparedStatement stmt = dbConnection().prepareStatement(query)) {
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
        try (PreparedStatement stmt = dbConnection().prepareStatement(query)) {

            int idOperatore = Integer.parseInt(userId.trim());
            stmt.setInt(1, idOperatore);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (NumberFormatException e) {
            throw new RemoteException("L'ID Operatore deve essere un numero valido.", e);
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la validazione delle credenziali.", e);
        }
    }

    public static void main(String[] args) {
        try {
            // Imposta il tema FlatDarkLaf
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Errore durante l'impostazione del tema: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }

        // Esegui la GUI
        SwingUtilities.invokeLater(() -> {
            try {
                new serverStart(); // Avvia la finestra serverStart
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Errore durante l'avvio dell'applicazione: " + e.getMessage());
            }
        });
    }
}