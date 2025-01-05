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
    public List<String> getAllMonitoringCenters() throws RemoteException {
        List<String> centers = new ArrayList<>();
        String query = "SELECT nome FROM centrimonitoraggio";

        try (Connection connection = dbConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                centers.add(rs.getString("nome"));
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero dei centri di monitoraggio", e);
        }
        return centers;
    }

    @Override
    public boolean registerOperator(String nome, String cognome, String codiceFiscale, String email, String username, String password, String centroMonitoraggio) throws RemoteException {
        String checkQuery = "SELECT COUNT(*) FROM operatoriregistrati WHERE codice_fiscale = ? OR email = ? OR username = ?";
        String getIdQuery = "SELECT id_centromonitoraggio FROM centrimonitoraggio WHERE nome = ?";
        String insertQuery = "INSERT INTO operatoriregistrati (nome, cognome, codice_fiscale, email, username, password, id_centromonitoraggio) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dbConnection()) {
            // Controllo duplicati
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, codiceFiscale);
                checkStmt.setString(2, email);
                checkStmt.setString(3, username);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    throw new RemoteException("Dati duplicati trovati per codice fiscale, email o username.");
                }
            }

            // Ottenere l'ID del centro monitoraggio
            int idCentroMonitoraggio;
            try (PreparedStatement getIdStmt = connection.prepareStatement(getIdQuery)) {
                getIdStmt.setString(1, centroMonitoraggio);
                ResultSet rs = getIdStmt.executeQuery();
                if (rs.next()) {
                    idCentroMonitoraggio = rs.getInt("id_centromonitoraggio");
                } else {
                    throw new RemoteException("Centro monitoraggio non trovato: " + centroMonitoraggio);
                }
            }

            // Inserimento dell'operatore
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, nome);
                insertStmt.setString(2, cognome);
                insertStmt.setString(3, codiceFiscale);
                insertStmt.setString(4, email);
                insertStmt.setString(5, username);
                insertStmt.setString(6, password);
                insertStmt.setInt(7, idCentroMonitoraggio);

                int rowsInserted = insertStmt.executeUpdate();
                return rowsInserted > 0;
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la registrazione dell'operatore", e);
        }
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
    public boolean validateCredentials(String username, String password) throws RemoteException {
        String query = "SELECT * FROM operatoriregistrati WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = dbConnection().prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            return rs.next(); // Ritorna true se le credenziali sono valide
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la validazione delle credenziali.", e);
        }
    }

    @Override
    public boolean registerArea(String nome, String nomeASCII, String stato, String statoCodice, double latitudine, double longitudine) throws RemoteException {
        String checkQuery = "SELECT COUNT(*) FROM coordinatemonitoraggio WHERE nome = ? AND latitudine = ? AND longitudine = ?";
        String insertQuery = "INSERT INTO coordinatemonitoraggio (nome, nome_ascii, stato, stato_code, latitudine, longitudine) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = dbConnection()) {
            // Controllo duplicati
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, nome);
                checkStmt.setDouble(2, latitudine);
                checkStmt.setDouble(3, longitudine);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    throw new RemoteException("Esiste già un'area con lo stesso nome, latitudine e longitudine.");
                }
            }

            // Inserimento dell'area di interesse
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, nome);
                insertStmt.setString(2, nomeASCII);
                insertStmt.setString(3, stato);
                insertStmt.setString(4, statoCodice);
                insertStmt.setDouble(5, latitudine);
                insertStmt.setDouble(6, longitudine);

                int rowsInserted = insertStmt.executeUpdate();
                return rowsInserted > 0;
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la registrazione dell'area di interesse", e);
        }
    }

    @Override
    public List<Map<String, String>> getLocationsForUser(String username) throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>();
        String query = "SELECT coordinatemonitoraggio.id_luogo, coordinatemonitoraggio.nome_ascii " +
                "FROM coordinatemonitoraggio " +
                "INNER JOIN centroarea ON coordinatemonitoraggio.id_luogo = centroarea.id_luogo " +
                "INNER JOIN operatoriregistrati ON centroarea.id_centromonitoraggio = operatoriregistrati.id_centromonitoraggio " +
                "WHERE operatoriregistrati.username = ? " +
                "ORDER BY coordinatemonitoraggio.nome_ascii ASC";

        System.out.println("**DEBUG** Esecuzione della query di getLocationsForUser");
        try (Connection connection = dbConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("id_luogo", String.valueOf(rs.getInt("id_luogo")));
                    row.put("nome_ascii", rs.getString("nome_ascii"));
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero delle aree per l'utente", e);
        }

        return results;
    }

    @Override
    public boolean addClimaticParameters(String username, String nomeArea, String data, String ora, int vento,
                                         int umidita, int pressione, int temperatura, int precipitazioni,
                                         int altitudine, int massa, String commentoVento, String commentoUmidita,
                                         String commentoPressione, String commentoTemperatura, String commentoPrecipitazioni,
                                         String commentoAltitudine, String commentoMassa) throws RemoteException {
        String query = "INSERT INTO parametriclimatici " +
                "(id_luogo, id_centromonitoraggio, data_di_rilevazione, ora, vento, umidita, pressione, temperatura, precipitazioni, altitudineghiacciai, massaghiacciai, " +
                "vento_nota, umidita_nota, pressione_nota, temperatura_nota, precipitazioni_nota, altitudineghiacciai_nota, massaghiacciai_nota) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dbConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            System.out.println("**DEBUG** Preparazione della query");

            // Recupera l'id del centro di monitoraggio associato all'operatore
            String centroQuery = "SELECT id_centromonitoraggio FROM operatoriregistrati WHERE username = ?";
            int idCentroMonitoraggio;

            try (PreparedStatement centroStmt = connection.prepareStatement(centroQuery)) {
                centroStmt.setString(1, username);
                ResultSet rs = centroStmt.executeQuery();
                if (rs.next()) {
                    idCentroMonitoraggio = rs.getInt("id_centromonitoraggio");
                    System.out.println("ID Centro Monitoraggio: " + idCentroMonitoraggio);
                } else {
                    throw new RemoteException("Centro di monitoraggio non trovato per l'utente: " + username);
                }
            }

            // Recupera l'id del luogo dall'area
            String luogoQuery = "SELECT id_luogo FROM coordinatemonitoraggio WHERE nome_ascii = ?";
            int idLuogo;

            try (PreparedStatement luogoStmt = connection.prepareStatement(luogoQuery)) {
                luogoStmt.setString(1, nomeArea);
                ResultSet rs = luogoStmt.executeQuery();
                if (rs.next()) {
                    idLuogo = rs.getInt("id_luogo");
                    System.out.println("ID Luogo: " + idLuogo);
                } else {
                    throw new RemoteException("Luogo non trovato per l'area: " + nomeArea);
                }
            }

            // Conversione della data e dell'ora
            String[] dateParts = data.split("/");
            String formattedDate = String.format("%s-%s-%s", dateParts[2], dateParts[1], dateParts[0]);
            String formattedTime = ora + ":00";
            System.out.println("Formatted Date: " + formattedDate);
            System.out.println("Formatted Time: " + formattedTime);

            // Assegna tutti i parametri al PreparedStatement
            stmt.setInt(1, idLuogo);
            stmt.setInt(2, idCentroMonitoraggio);
            stmt.setDate(3, java.sql.Date.valueOf(formattedDate));
            stmt.setTime(4, java.sql.Time.valueOf(formattedTime));
            stmt.setInt(5, vento);
            stmt.setInt(6, umidita);
            stmt.setInt(7, pressione);
            stmt.setInt(8, temperatura);
            stmt.setInt(9, precipitazioni);
            stmt.setInt(10, altitudine);
            stmt.setInt(11, massa);
            stmt.setString(12, commentoVento != null ? commentoVento : ""); // Default: stringa vuota
            stmt.setString(13, commentoUmidita != null ? commentoUmidita : "");
            stmt.setString(14, commentoPressione != null ? commentoPressione : "");
            stmt.setString(15, commentoTemperatura != null ? commentoTemperatura : "");
            stmt.setString(16, commentoPrecipitazioni != null ? commentoPrecipitazioni : "");
            stmt.setString(17, commentoAltitudine != null ? commentoAltitudine : "");
            stmt.setString(18, commentoMassa != null ? commentoMassa : "");

            System.out.println("**DEBUG** Esecuzione della query di insert");
            int rowsInserted = stmt.executeUpdate();
            System.out.println("**DEBUG** Rows inserted: " + rowsInserted);
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("**DEBUG** SQLException: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Errore durante l'inserimento dei parametri climatici", e);
        } catch (IllegalArgumentException e) {
            System.out.println("**DEBUG** IllegalArgumentException: " + e.getMessage());
            throw new RemoteException("Errore nel formato della data o dell'ora", e);
        }
    }

    @Override
    public boolean checkExistingClimaticParameter(String nomeArea, String data, String ora) throws RemoteException {
        String query = "SELECT COUNT(*) FROM parametriclimatici WHERE id_luogo = (SELECT id_luogo FROM coordinatemonitoraggio WHERE nome_ascii = ?) AND data_di_rilevazione = ? AND ora = ?";
        try (Connection connection = dbConnection(); PreparedStatement stmt = connection.prepareStatement(query)) {
            // Converti la data dal formato dd/MM/yyyy a yyyy-MM-dd
            String[] dateParts = data.split("/");
            String formattedDate = String.format("%s-%s-%s", dateParts[2], dateParts[1], dateParts[0]); // yyyy-MM-dd

            stmt.setString(1, nomeArea);
            stmt.setDate(2, java.sql.Date.valueOf(formattedDate)); // Usa il formato corretto
            stmt.setTime(3, java.sql.Time.valueOf(ora + ":00")); // Aggiungi ":00" per il formato SQL

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // True se esiste già
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la verifica del parametro climatico esistente.", e);
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            throw new RemoteException("Errore nel formato della data o dell'ora fornita.", e);
        }
        return false;
    }

    @Override
    public List<Map<String, String>> getClimaticDataById(int areaId) throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>();
        String query = "SELECT id_parametro, data_di_rilevazione, ora, vento, umidita, pressione, " +
                "temperatura, precipitazioni, altitudineghiacciai, massaghiacciai " +
                "FROM parametriclimatici WHERE id_luogo = ? ORDER BY data_di_rilevazione DESC, ora DESC";

        try (Connection connection = dbConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, areaId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id_parametro", String.valueOf(rs.getInt("id_parametro"))); // ID Parametro
                row.put("data_di_rilevazione", rs.getDate("data_di_rilevazione").toString());
                row.put("ora", rs.getTime("ora").toString());
                row.put("vento", String.valueOf(rs.getInt("vento")));
                row.put("umidita", String.valueOf(rs.getInt("umidita")));
                row.put("pressione", String.valueOf(rs.getInt("pressione")));
                row.put("temperatura", String.valueOf(rs.getInt("temperatura")));
                row.put("precipitazioni", String.valueOf(rs.getInt("precipitazioni")));
                row.put("altitudineghiacciai", String.valueOf(rs.getInt("altitudineghiacciai")));
                row.put("massaghiacciai", String.valueOf(rs.getInt("massaghiacciai")));

                System.out.println("DEBUG (Server): ID Parametro = " + row.get("id_parametro"));
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero dei dati climatici", e);
        }

        return results;
    }

    @Override
    public Map<String, Double> getAveragesById(int areaId) throws RemoteException {
        Map<String, Double> averages = new HashMap<>();
        String query = "SELECT AVG(vento) AS vento_media, AVG(umidita) AS umidita_media, " +
                "AVG(pressione) AS pressione_media, AVG(temperatura) AS temperatura_media, " +
                "AVG(precipitazioni) AS precipitazioni_media, AVG(altitudineghiacciai) AS altitudine_media, " +
                "AVG(massaghiacciai) AS massa_media FROM parametriclimatici WHERE id_luogo = ?";

        try (Connection connection = dbConnection(); PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, areaId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                averages.put("vento", rs.getDouble("vento_media"));
                averages.put("umidita", rs.getDouble("umidita_media"));
                averages.put("pressione", rs.getDouble("pressione_media"));
                averages.put("temperatura", rs.getDouble("temperatura_media"));
                averages.put("precipitazioni", rs.getDouble("precipitazioni_media"));
                averages.put("altitudineghiacciai", rs.getDouble("altitudine_media"));
                averages.put("massaghiacciai", rs.getDouble("massa_media"));
                System.out.println("DEBUG: Media calcolata = " + averages);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Errore durante il calcolo delle medie climatiche", e);
        }

        return averages;
    }

    @Override
    public Map<String, Integer> getModesById(int areaId) throws RemoteException {
        Map<String, Integer> modes = new HashMap<>();
        String[] parameters = {"vento", "umidita", "pressione", "temperatura", "precipitazioni", "altitudineghiacciai", "massaghiacciai"};

        try (Connection connection = dbConnection()) {
            for (String parameter : parameters) {
                String query = "SELECT " + parameter + ", COUNT(" + parameter + ") AS occorrenze " +
                        "FROM parametriclimatici WHERE id_luogo = ? " +
                        "GROUP BY " + parameter + " " +
                        "ORDER BY occorrenze DESC, " + parameter + " ASC LIMIT 1";

                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, areaId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        modes.put(parameter, rs.getInt(parameter));
                    } else {
                        modes.put(parameter, 0); // Default se non ci sono dati
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Errore durante il calcolo delle mode climatiche", e);
        }

        System.out.println("DEBUG: Modes calcolate = " + modes);
        return modes;
    }

    @Override
    public Map<String, Double> getMediansById(int areaId) throws RemoteException {
        Map<String, Double> medians = new HashMap<>();
        String[] parameters = {"vento", "umidita", "pressione", "temperatura", "precipitazioni", "altitudineghiacciai", "massaghiacciai"};

        try (Connection connection = dbConnection()) {
            for (String parameter : parameters) {
                String query = "SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY " + parameter + ") AS mediana " +
                        "FROM parametriclimatici WHERE id_luogo = ?";

                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, areaId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        medians.put(parameter, rs.getDouble("mediana"));
                    } else {
                        medians.put(parameter, 0.0); // Default se non ci sono dati
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Errore durante il calcolo delle mediane climatiche", e);
        }

        System.out.println("DEBUG: Medians calcolate = " + medians);
        return medians;
    }


    @Override
    public String getCommentForParameterById(int idParametro, String parameterNoteColumn) throws RemoteException {
        String query = "SELECT " + parameterNoteColumn + " AS commento " +
                "FROM parametriclimatici " +
                "WHERE id_parametro = ?";
        try (Connection connection = dbConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, idParametro); // Usa id_parametro come filtro
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("commento") != null ? rs.getString("commento") : "Nessun commento disponibile.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Errore durante il recupero del commento", e);
        }
        return "Nessun commento disponibile.";
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