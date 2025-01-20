package org.example;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.*;

/**
 * Classe principale per l'avvio del server di Climate Monitoring.
 * Gestisce la configurazione e l'avvio del server RMI e la connessione al database.
 *
 * @author Agliati Lorenzo 753378
 */
public class serverCM extends JFrame implements ClimateInterface {

    /**
     * Bottone per avviare il server.
     */
    private JButton startServerButton;

    /**
     * Pannello principale della GUI.
     */
    private JPanel serverStartPanel;

    /**
     * Bottone per fermare il server.
     */
    private JButton stopServerButton;

    /**
     * Etichetta per lo stato del server.
     */
    private JLabel status;

    /**
     * Campo di testo per l'IP e la porta del database.
     */
    private JTextField textfieldIPandPORT;
    private JTextField textFieldDBnome;
    private JTextField textFieldUser;
    private JTextField textFieldPassword;
    private JButton buttonCreateDB;

    /**
     * Connessione al database.
     */
    private Connection conn;

    /**
     * Indica se il server è in esecuzione.
     */
    private boolean isServerRunning = false;

    /**
     * Costruttore della classe serverCM.
     * Inizializza la GUI e i listener degli eventi.
     */
    public serverCM() {
        setContentPane(serverStartPanel); // Pannello principale della GUI
        setTitle("Server Climate Monitoring"); // Imposta il titolo della finestra
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Configura l'azione di chiusura
        setSize(600, 400); // Dimensioni della finestra
        setLocationRelativeTo(null); // Centra la finestra sullo schermo
        setVisible(true); // Mostra la finestra

        textfieldIPandPORT.setText("localhost:5432"); // Imposta il valore di default per IP e porta

        // Listener per avviare il server
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isServerRunning) {
                    String ipAndPort = textfieldIPandPORT.getText().trim();
                    if (ipAndPort.isEmpty() || !ipAndPort.matches("^\\S+:\\d+$")) {
                        JOptionPane.showMessageDialog(serverCM.this, "Inserire un valore valido nel formato IP:PORT.", "Errore", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String dbName = textFieldDBnome.getText().trim();
                    if (dbName.isEmpty()) {
                        JOptionPane.showMessageDialog(serverCM.this, "Il nome del database non può essere vuoto.", "Errore", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Verifica le credenziali del database
                    if (!validateDatabaseCredentials()) {
                        JOptionPane.showMessageDialog(serverCM.this, "Credenziali del database non valide. Verifica nome database, utente e password.", "Errore", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    try {
                        System.out.println("Valore inserito nella textfieldIPandPORT: " + ipAndPort); // Debug: mostra il valore
                        startRmiServer(); // Avvia il server RMI
                        JOptionPane.showMessageDialog(serverCM.this, "Server avviato con successo!");

                        // Disabilita le text field dopo l'avvio del server
                        disableThings();

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(serverCM.this, "Errore durante l'avvio del server: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(serverCM.this, "Il server è già in esecuzione!");
                }
            }
        });

        // Listener per fermare il server
        stopServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isServerRunning) {
                    isServerRunning = false; // Cambia lo stato del server
                    status.setText("Status: Server fermo."); // Aggiorna l'etichetta dello stato
                    JOptionPane.showMessageDialog(serverCM.this, "Server arrestato."); // Messaggio di conferma
                    System.exit(0); // Chiude l'applicazione
                } else {
                    JOptionPane.showMessageDialog(serverCM.this, "Il server è già fermo!");
                }
            }
        });

        // Listener per creare il database mediante query
        buttonCreateDB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createDatabaseAndTables();
            }
        });
    }

    /**
     * Disabilita tutte le text field nel pannello principale.
     */
    private void disableThings() {
        textfieldIPandPORT.setEnabled(false);
        textFieldDBnome.setEnabled(false);
        textFieldUser.setEnabled(false);
        textFieldPassword.setEnabled(false);
        buttonCreateDB.setEnabled(false);
        startServerButton.setEnabled(false);
        stopServerButton.setEnabled(true);
    }

    /**
     * Crea il database e le tabelle necessarie per il funzionamento del server.
     */
    private void createDatabaseAndTables() {
        // Ottieni i valori di IP:Port, nome database, user e password dalle text field
        String ipAndPort = textfieldIPandPORT.getText().trim();
        String dbName = textFieldDBnome.getText().trim();
        String user = textFieldUser.getText().trim();
        String password = textFieldPassword.getText().trim();

        // Verifica che i campi non siano vuoti
        if (ipAndPort.isEmpty() || dbName.isEmpty() || user.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(serverCM.this, "Inserire tutti i dati richiesti: IP:Port, nome database, utente e password.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Mostra un pop-up di conferma
        int confirm = JOptionPane.showConfirmDialog(
                serverCM.this,
                "Sei sicuro di voler creare un nuovo database?\n" +
                        "Continuando verrà creato un nuovo database, verranno utilizzati i valori inseriti nelle textfield.\n" +
                        "Il server verrà avviato in automatico con le impostazioni delle textfield attuali, questo per permettere l'inserimento delle aree geografiche di default prese dal file coordinatemonitoraggio_defaultdata.csv\n" +
                        "Si consiglia di lasciare i valori di base nelle textfield.\n" +
                        "Attendere la visualizzazione del messaggio di conclusione del processo: Server avviato con successo!",
                "Conferma creazione database",
                JOptionPane.YES_NO_OPTION
        );

        // Se l'utente non conferma, esci dal metodo
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Costruisci l'URL di connessione al database PostgreSQL
        String url = "jdbc:postgresql://" + ipAndPort + "/postgres"; // Connessione al database di default "postgres"

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            // Verifica se il database esiste già
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
            if (rs.next()) {
                JOptionPane.showMessageDialog(serverCM.this, "Il database " + dbName + " esiste già.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return; // Esci dal metodo se il database esiste già
            }

            // Crea il database
            stmt.executeUpdate("CREATE DATABASE " + dbName);

            // Connessione al database appena creato
            String dbUrl = "jdbc:postgresql://" + ipAndPort + "/" + dbName;
            try (Connection dbConn = DriverManager.getConnection(dbUrl, user, password);
                 Statement dbStmt = dbConn.createStatement()) {

                // Crea le tabelle con le foreign key
                dbStmt.executeUpdate("CREATE TABLE IF NOT EXISTS centrimonitoraggio (" +
                        "id_centromonitoraggio SERIAL PRIMARY KEY, " +
                        "nome VARCHAR(100), " +
                        "indirizzo VARCHAR(100))");

                dbStmt.executeUpdate("CREATE TABLE IF NOT EXISTS coordinatemonitoraggio (" +
                        "id_luogo SERIAL PRIMARY KEY, " +
                        "latitudine NUMERIC(9,6), " +
                        "longitudine NUMERIC(9,6), " +
                        "nome VARCHAR(100), " +
                        "stato VARCHAR(50), " +
                        "nome_ascii VARCHAR(100), " +
                        "stato_code VARCHAR(2))");

                dbStmt.executeUpdate("CREATE TABLE IF NOT EXISTS operatoriregistrati (" +
                        "id_operatore SERIAL PRIMARY KEY, " +
                        "nome VARCHAR(50), " +
                        "cognome VARCHAR(50), " +
                        "codice_fiscale VARCHAR(16), " +
                        "email VARCHAR(100), " +
                        "password VARCHAR(255), " +
                        "id_centromonitoraggio INTEGER REFERENCES centrimonitoraggio(id_centromonitoraggio), " +
                        "username VARCHAR(50))");

                dbStmt.executeUpdate("CREATE TABLE IF NOT EXISTS centroarea (" +
                        "id_integer SERIAL PRIMARY KEY, " +
                        "id_centromonitoraggio INTEGER REFERENCES centrimonitoraggio(id_centromonitoraggio), " +
                        "id_luogo INTEGER REFERENCES coordinatemonitoraggio(id_luogo))");

                dbStmt.executeUpdate("CREATE TABLE IF NOT EXISTS parametriclimatici (" +
                        "id_parametro SERIAL PRIMARY KEY, " +
                        "id_luogo INTEGER REFERENCES coordinatemonitoraggio(id_luogo), " +
                        "id_centromonitoraggio INTEGER REFERENCES centrimonitoraggio(id_centromonitoraggio), " +
                        "data_di_rilevazione DATE, " +
                        "ora TIME WITHOUT TIME ZONE, " +
                        "vento INTEGER, " +
                        "vento_nota VARCHAR(256), " +
                        "umidita INTEGER, " +
                        "umidita_nota VARCHAR(256), " +
                        "pressione INTEGER, " +
                        "pressione_nota VARCHAR(256), " +
                        "temperatura INTEGER, " +
                        "temperatura_nota VARCHAR(256), " +
                        "precipitazioni INTEGER, " +
                        "precipitazioni_nota VARCHAR(256), " +
                        "altitudineghiacciai INTEGER, " +
                        "altitudineghiacciai_nota VARCHAR(256), " +
                        "massaghiacciai INTEGER, " +
                        "massaghiacciai_nota VARCHAR(256))");

                JOptionPane.showMessageDialog(serverCM.this, "Database e tabelle creati con successo!");

                // Importa i dati dal file CSV
                importDefaultDataFromCSV(dbConn);

                // Avvia il server automaticamente
                try {
                    startRmiServer();
                    JOptionPane.showMessageDialog(serverCM.this, "Server avviato con successo!");
                } catch (RemoteException e) {
                    JOptionPane.showMessageDialog(serverCM.this, "Errore durante l'avvio del server: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }

                // Disabilita le text field dopo l'avvio del server
                disableThings();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(serverCM.this, "Errore durante la creazione delle tabelle: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(serverCM.this, "Errore durante la creazione del database: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Importa i dati delle aree di monitoraggio da un file CSV, le aree dentro suddetto file sono le aree fornite coi pdf che descrivono le specifiche del progetto. Come richiesto dalle specifiche del progetto sono solo le are dello stato italiano.
     * @throws RemoteException In caso di errore.
     */
    private void importDefaultDataFromCSV(Connection conn) {
        String csvFile = "coordinatemonitoraggio_defaultdata.csv"; // Percorso del file CSV
        String line;
        String csvSplitBy = ","; // Separatore utilizzato nel file CSV

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            // Salta la prima riga (header)
            br.readLine();

            // Leggi il file riga per riga
            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy);

                // Estrai i dati dalla riga
                String geonameId = data[0].trim();
                String name = data[1].trim();
                String asciiName = data[2].trim();
                String countryCode = data[3].trim();
                String countryName = data[4].trim();
                double latitude = Double.parseDouble(data[5].trim());
                double longitude = Double.parseDouble(data[6].trim());

                // Inserisci i dati nella tabella coordinatemonitoraggio
                String insertQuery = "INSERT INTO coordinatemonitoraggio (nome, nome_ascii, stato, stato_code, latitudine, longitudine) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, asciiName);
                    pstmt.setString(3, countryName);
                    pstmt.setString(4, countryCode);
                    pstmt.setDouble(5, latitude);
                    pstmt.setDouble(6, longitude);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(serverCM.this, "Errore durante l'inserimento dei dati: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(serverCM.this, "Errore durante la lettura del file CSV: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Avvia il server e registra il servizio RMI.
     * Configura le connessioni e imposta lo stato del server.
     * @throws RemoteException Se si verifica un errore di comunicazione.
     */
    private void startRmiServer() throws RemoteException {
        ClimateInterface stub = (ClimateInterface) UnicastRemoteObject.exportObject(this, 0); // Esporta l'oggetto RMI
        Registry registry = LocateRegistry.createRegistry(1099); // Crea il registro RMI sulla porta 1099
        registry.rebind("ClimateService", stub); // Registra il servizio RMI con il nome "ClimateService"
        isServerRunning = true; // Aggiorna lo stato del server
        status.setText("Status: Server in esecuzione..."); // Aggiorna lo stato nella GUI
        System.out.println("Server RMI avviato e registrato."); // Debug: server avviato
    }

    /**
     * Costruisce l'URL del database utilizzando i dati forniti.
     * @return Stringa contenente l'URL del database.
     */
    private String getDatabaseUrl() {
        String ipAndPort = textfieldIPandPORT.getText().trim(); // Ottiene l'IP e la porta dal campo di testo
        String dbName = textFieldDBnome.getText().trim(); // Ottiene il nome del database
        System.out.println("DEBUG (Server): ipAndPort = " + ipAndPort + " dbName = " + dbName); // Debug per monitorare i dati
        return "jdbc:postgresql://" + ipAndPort + "/" + dbName; // Costruisce l'URL del database (climatedb)
    }

    /**
     * Stabilisce una connessione al database.
     * @return Oggetto Connection per l'accesso al database.
     * @throws SQLException In caso di errore durante la connessione.
     */
    private Connection dbConnection() throws SQLException {
        if (conn == null || conn.isClosed()) { // Verifica se la connessione è chiusa
            String url = getDatabaseUrl(); // Ottiene l'URL del database
            String user = textFieldUser.getText().trim(); // Ottiene il nome utente per il database
            String password = textFieldPassword.getText().trim(); // Ottiene la password dell utente per il database
            conn = DriverManager.getConnection(url, user, password); // Connette al database
            //System.out.println("DEBUG (Server): user = " + user + " password = " + password);
            System.out.println("Connessione al database avvenuta con successo."); // Debug: connessione avvenuta
        }
        return conn; // Ritorna la connessione attiva
    }

    /**
     * Verifica se le credenziali del database sono valide tentando di stabilire una connessione.
     * @return True se la connessione ha successo, false altrimenti.
     */
    private boolean validateDatabaseCredentials() {
        try {
            Connection testConn = DriverManager.getConnection(getDatabaseUrl(), textFieldUser.getText().trim(), textFieldPassword.getText().trim());
            testConn.close(); // Chiudi la connessione di test
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(serverCM.this, "Errore di connessione al database: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Recupera tutti i dati delle aree monitorate, inclusi ID e nomi ASCII.
     * @return Lista di mappe contenenti ID e nome ASCII delle aree monitorate.
     * @throws RemoteException In caso di errore durante la comunicazione con il database.
     */
    @Override
    public List<Map<String, String>> getAllData() throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>(); // Lista dei risultati
        String query = "SELECT id_luogo, nome_ascii FROM coordinatemonitoraggio ORDER BY nome_ascii ASC"; // Query SQL per recuperare i dati

        try (Connection connection = dbConnection(); // Ottiene la connessione al database
             Statement stmt = connection.createStatement(); // Crea uno statement SQL
             ResultSet rs = stmt.executeQuery(query)) { // Esegue la query e ottiene il risultato

            while (rs.next()) { // Itera sui risultati della query
                Map<String, String> row = new HashMap<>(); // Mappa per memorizzare i dati della riga
                row.put("id_luogo", String.valueOf(rs.getInt("id_luogo"))); // Aggiunge l'ID del luogo
                row.put("nome_ascii", rs.getString("nome_ascii")); // Aggiunge il nome ASCII
                results.add(row); // Aggiunge la mappa alla lista
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero dei dati", e); // Propaga l'eccezione come RemoteException
        }

        return results; // Ritorna i risultati
    }

    /**
     * Recupera i dati minimali delle aree monitorate, inclusi ID e nomi ASCII.
     * Questo metodo è una versione ridotta di getAllData.
     * @return Lista di mappe contenenti ID e nome ASCII delle aree monitorate.
     * @throws RemoteException In caso di errore durante la comunicazione con il database.
     */
    @Override
    public List<Map<String, String>> getMinimalLocationData() throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>(); // Lista dei risultati
        String query = "SELECT id_luogo, nome_ascii FROM coordinatemonitoraggio ORDER BY nome_ascii ASC"; // Query SQL

        try (Connection connection = dbConnection(); // Ottiene la connessione al database
             Statement stmt = connection.createStatement(); // Crea uno statement SQL
             ResultSet rs = stmt.executeQuery(query)) { // Esegue la query

            while (rs.next()) { // Itera sui risultati
                Map<String, String> row = new HashMap<>(); // Mappa per memorizzare i dati della riga
                row.put("id_luogo", String.valueOf(rs.getInt("id_luogo"))); // Aggiunge l'ID del luogo
                row.put("nome_ascii", rs.getString("nome_ascii")); // Aggiunge il nome ASCII
                results.add(row); // Aggiunge la mappa alla lista
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero dei dati minimali", e); // Propaga l'eccezione come RemoteException
        }

        return results; // Ritorna i risultati
    }

    /**
     * Registra un nuovo centro di monitoraggio nel sistema, associa il centro alle aree specificate e, se richiesto, aggiorna il centro di monitoraggio associato all'operatore.
     * @param name              Nome del centro di monitoraggio.
     * @param address           Indirizzo del centro di monitoraggio.
     * @param areaIds           Lista degli ID delle aree da associare al centro.
     * @param isCheckboxChecked Indica se il centro di monitoraggio deve essere associato all'operatore.
     * @param username          Nome utente dell'operatore al quale associare il centro.
     * @return {@code true} se la registrazione ha successo, {@code false} altrimenti.
     * @throws RemoteException Se si verifica un errore di comunicazione o durante l'esecuzione della query.
     */
    @Override
    public boolean registerMonitoringCenter(String name, String address, List<Integer> areaIds, boolean isCheckboxChecked, String username) throws RemoteException {
        String insertCenterQuery = "INSERT INTO centrimonitoraggio (nome, indirizzo) VALUES (?, ?) RETURNING id_centromonitoraggio"; // Query per inserire il centro
        String updateOperatorQuery = "UPDATE operatoriregistrati SET id_centromonitoraggio = ? WHERE username = ?"; // Query per aggiornare l'operatore
        String insertAssociationQuery = "INSERT INTO centroarea (id_centromonitoraggio, id_luogo) VALUES (?, ?)"; // Query per associare le aree

        Connection connection = null;

        try {
            connection = dbConnection(); // Ottiene la connessione al database
            connection.setAutoCommit(false); // Avvia una transazione

            int centerId;

            // Creazione del centro di monitoraggio
            try (PreparedStatement centerStmt = connection.prepareStatement(insertCenterQuery)) {
                centerStmt.setString(1, name);
                centerStmt.setString(2, address);
                try (ResultSet rs = centerStmt.executeQuery()) {
                    if (rs.next()) {
                        centerId = rs.getInt("id_centromonitoraggio"); // Recupera l'ID del centro
                    } else {
                        connection.rollback();
                        throw new SQLException("Errore nel recuperare l'ID del centro di monitoraggio appena inserito.");
                    }
                }
            }

            // Inserisce la relazione tra il centro e le aree
            try (PreparedStatement assocStmt = connection.prepareStatement(insertAssociationQuery)) {
                for (int areaId : areaIds) {
                    assocStmt.setInt(1, centerId); // Imposta l'ID del centro
                    assocStmt.setInt(2, areaId); // Imposta l'ID dell'area
                    assocStmt.executeUpdate(); // Esegue l'inserimento
                }
            }

            // Se la checkbox è selezionata, associa il centro all'operatore
            if (isCheckboxChecked) {
                try (PreparedStatement updateStmt = connection.prepareStatement(updateOperatorQuery)) {
                    updateStmt.setInt(1, centerId);
                    updateStmt.setString(2, username); // Usa l'username anziché l'ID
                    updateStmt.executeUpdate();
                }
            }

            connection.commit(); // Conclude la transazione
            return true;

        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback(); // Annulla la transazione in caso di errore
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            throw new RemoteException("Errore durante la registrazione del centro di monitoraggio.", e);

        } finally {
            if (connection != null) {
                try {
                    connection.close(); // Chiude la connessione
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    /**
     * Controlla se esiste un centro di monitoraggio con lo stesso nome o indirizzo.
     * @param name Nome del centro di monitoraggio da verificare.
     * @param address Indirizzo del centro di monitoraggio da verificare.
     * @return True se esiste un duplicato, false altrimenti.
     * @throws RemoteException In caso di errore durante la comunicazione con il database.
     */
    @Override
    public boolean checkDuplicateMonitoringCenter(String name, String address) throws RemoteException {
        String query = "SELECT COUNT(*) FROM centrimonitoraggio WHERE nome = ? OR indirizzo = ?"; // Query SQL per verificare duplicati
        try (Connection connection = dbConnection(); // Ottiene la connessione al database
             PreparedStatement stmt = connection.prepareStatement(query)) { // Prepara la query
            stmt.setString(1, name); // Imposta il nome
            stmt.setString(2, address); // Imposta l'indirizzo
            ResultSet rs = stmt.executeQuery(); // Esegue la query
            if (rs.next()) {
                return rs.getInt(1) > 0; // Restituisce true se esiste un duplicato
            }
        } catch (SQLException e) {
            System.err.println("Errore SQL: " + e.getMessage()); // Log di errore
            e.printStackTrace(); // Stampa dettagliata per il debug
            throw new RemoteException("Errore generico durante il controllo dei duplicati"); // Propaga l'errore come RemoteException
        }
        return false; // Nessun duplicato trovato
    }

    /**
     * Recupera tutti i centri di monitoraggio registrati.
     * @return Lista di stringhe contenenti i nomi dei centri di monitoraggio.
     * @throws RemoteException In caso di errore durante la comunicazione con il database.
     */
    @Override
    public List<String> getAllMonitoringCenters() throws RemoteException {
        List<String> centers = new ArrayList<>(); // Lista per memorizzare i nomi dei centri
        String query = "SELECT nome FROM centrimonitoraggio"; // Query SQL per ottenere i centri

        try (Connection connection = dbConnection(); // Ottiene la connessione al database
             Statement stmt = connection.createStatement(); // Crea uno statement SQL
             ResultSet rs = stmt.executeQuery(query)) { // Esegue la query

            while (rs.next()) { // Itera sui risultati della query
                centers.add(rs.getString("nome")); // Aggiunge il nome del centro alla lista
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero dei centri di monitoraggio", e); // Propaga l'errore come RemoteException
        }
        return centers; // Ritorna la lista dei centri
    }

    /**
     * Registra un nuovo operatore e lo associa a un centro di monitoraggio.
     * @param nome Nome dell'operatore.
     * @param cognome Cognome dell'operatore.
     * @param codiceFiscale Codice fiscale dell'operatore.
     * @param email Email dell'operatore.
     * @param username Username per l'accesso dell'operatore.
     * @param password Password per l'accesso dell'operatore.
     * @param centroMonitoraggio Nome del centro di monitoraggio associato.
     * @return True se la registrazione ha successo, false altrimenti.
     * @throws RemoteException In caso di errore durante la comunicazione o l'operazione sul database.
     */
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

            // Ottenere l'ID del centro monitoraggio (se selezionato)
            Integer idCentroMonitoraggio = null;
            if (centroMonitoraggio != null && !centroMonitoraggio.isEmpty()) {
                try (PreparedStatement getIdStmt = connection.prepareStatement(getIdQuery)) {
                    getIdStmt.setString(1, centroMonitoraggio);
                    ResultSet rs = getIdStmt.executeQuery();
                    if (rs.next()) {
                        idCentroMonitoraggio = rs.getInt("id_centromonitoraggio");
                    } else {
                        throw new RemoteException("Centro monitoraggio non trovato: " + centroMonitoraggio);
                    }
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
                if (idCentroMonitoraggio != null) {
                    insertStmt.setInt(7, idCentroMonitoraggio);
                } else {
                    insertStmt.setNull(7, java.sql.Types.INTEGER); // Imposta il valore NULL per id_centromonitoraggio
                }

                int rowsInserted = insertStmt.executeUpdate();
                return rowsInserted > 0;
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la registrazione dell'operatore", e);
        }
    }

    /**
     * Effettua una ricerca di aree geografiche entro un determinato raggio usando latitudine e longitudine.
     * @param latitude Latitudine del punto di ricerca.
     * @param longitude Longitudine del punto di ricerca.
     * @param radius Raggio di ricerca in chilometri.
     * @return Lista di mappe con ID, latitudine, longitudine, nome ASCII e distanza delle aree trovate.
     * @throws RemoteException In caso di errore durante la comunicazione con il database.
     */
    @Override
    public List<Map<String, String>> searchByCoordinates(double latitude, double longitude, double radius) throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>(); // Lista per memorizzare i risultati della ricerca

        String query = "SELECT id_luogo, latitudine, longitudine, nome_ascii, " +
                "(6371 * acos(cos(radians(?)) * cos(radians(latitudine)) * " +
                "cos(radians(longitudine) - radians(?)) + sin(radians(?)) * sin(radians(latitudine)))) AS distance " +
                "FROM coordinatemonitoraggio " +
                "WHERE (6371 * acos(cos(radians(?)) * cos(radians(latitudine)) * " +
                "cos(radians(longitudine) - radians(?)) + sin(radians(?)) * sin(radians(latitudine)))) <= ? " +
                "ORDER BY distance";

        try (PreparedStatement stmt = dbConnection().prepareStatement(query)) { // Prepara la query SQL
            stmt.setDouble(1, latitude); // Imposta la latitudine
            stmt.setDouble(2, longitude); // Imposta la longitudine
            stmt.setDouble(3, latitude); // Imposta di nuovo la latitudine per il calcolo
            stmt.setDouble(4, latitude); // Ripete la latitudine per la query
            stmt.setDouble(5, longitude); // Imposta di nuovo la longitudine
            stmt.setDouble(6, latitude); // Ripete la latitudine per la query
            stmt.setDouble(7, radius); // Imposta il raggio di ricerca

            ResultSet rs = stmt.executeQuery(); // Esegue la query

            while (rs.next()) { // Itera sui risultati della query
                Map<String, String> row = new HashMap<>(); // Mappa per memorizzare i dati della riga
                row.put("id_luogo", String.valueOf(rs.getInt("id_luogo"))); // ID del luogo
                row.put("latitudine", String.valueOf(rs.getDouble("latitudine"))); // Latitudine
                row.put("longitudine", String.valueOf(rs.getDouble("longitudine"))); // Longitudine
                row.put("nome_ascii", rs.getString("nome_ascii")); // Nome ASCII
                row.put("distance", String.format("%.2f", rs.getDouble("distance"))); // Distanza
                results.add(row); // Aggiunge la mappa alla lista dei risultati
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la ricerca per coordinate con raggio", e); // Propaga l'eccezione come RemoteException
        }

        return results; // Ritorna i risultati della ricerca
    }

    /**
     * Effettua una ricerca di aree geografiche in base al nome fornito.
     * @param name Nome o parte del nome dell'area da cercare (case insensitive).
     * @return Lista di mappe contenenti ID, latitudine, longitudine e nome ASCII delle aree trovate.
     * @throws RemoteException In caso di errore durante la comunicazione con il database.
     */
    @Override
    public List<Map<String, String>> searchByName(String name) throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>(); // Lista per memorizzare i risultati della ricerca
        String query = "SELECT * FROM coordinatemonitoraggio WHERE nome_ascii ILIKE ?"; // Query SQL per la ricerca

        try (PreparedStatement stmt = dbConnection().prepareStatement(query)) { // Prepara la query SQL
            stmt.setString(1, "%" + name + "%"); // Imposta il parametro per la ricerca (case insensitive)
            ResultSet rs = stmt.executeQuery(); // Esegue la query

            while (rs.next()) { // Itera sui risultati della query
                Map<String, String> row = new HashMap<>(); // Mappa per memorizzare i dati della riga
                row.put("id_luogo", String.valueOf(rs.getInt("id_luogo"))); // ID del luogo
                row.put("latitudine", String.valueOf(rs.getDouble("latitudine"))); // Latitudine
                row.put("longitudine", String.valueOf(rs.getDouble("longitudine"))); // Longitudine
                row.put("nome_ascii", rs.getString("nome_ascii")); // Nome ASCII
                results.add(row); // Aggiunge la mappa alla lista dei risultati
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la ricerca per nome", e); // Propaga l'eccezione come RemoteException
        }

        return results; // Ritorna i risultati della ricerca
    }

    /**
     * Valida le credenziali di un operatore.
     * @param username Username dell'operatore.
     * @param password Password dell'operatore.
     * @return True se le credenziali sono valide, false altrimenti.
     * @throws RemoteException In caso di errore durante la comunicazione con il database.
     */
    @Override
    public boolean validateCredentials(String username, String password) throws RemoteException {
        String query = "SELECT * FROM operatoriregistrati WHERE username = ? AND password = ?"; // Query SQL per la validazione delle credenziali
        try (PreparedStatement stmt = dbConnection().prepareStatement(query)) { // Prepara la query SQL
            stmt.setString(1, username); // Imposta l'username
            stmt.setString(2, password); // Imposta la password

            ResultSet rs = stmt.executeQuery(); // Esegue la query
            return rs.next(); // Ritorna true se le credenziali sono valide
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la validazione delle credenziali.", e); // Propaga l'eccezione come RemoteException
        }
    }

    /**
     * Registra una nuova area di interesse.
     * @param nome Nome dell'area.
     * @param nomeASCII Nome ASCII dell'area.
     * @param stato Stato in cui si trova l'area.
     * @param statoCodice Codice dello stato.
     * @param latitudine Latitudine dell'area.
     * @param longitudine Longitudine dell'area.
     * @return True se l'area è stata registrata con successo, false altrimenti.
     * @throws RemoteException In caso di errore durante la comunicazione con il database.
     */
    @Override
    public boolean registerArea(String nome, String nomeASCII, String stato, String statoCodice, double latitudine, double longitudine) throws RemoteException {
        String checkQuery = "SELECT COUNT(*) FROM coordinatemonitoraggio WHERE nome = ? AND latitudine = ? AND longitudine = ?"; // Query per controllare duplicati
        String insertQuery = "INSERT INTO coordinatemonitoraggio (nome, nome_ascii, stato, stato_code, latitudine, longitudine) VALUES (?, ?, ?, ?, ?, ?)"; // Query per inserire l'area

        try (Connection connection = dbConnection()) { // Ottiene la connessione al database
            // Controllo duplicati
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) { // Prepara la query di controllo
                checkStmt.setString(1, nome); // Imposta il nome
                checkStmt.setDouble(2, latitudine); // Imposta la latitudine
                checkStmt.setDouble(3, longitudine); // Imposta la longitudine
                ResultSet rs = checkStmt.executeQuery(); // Esegue la query

                if (rs.next() && rs.getInt(1) > 0) { // Verifica se esistono duplicati
                    throw new RemoteException("Esiste già un'area con lo stesso nome, latitudine e longitudine."); // Propaga l'errore
                }
            }

            // Inserimento dell'area di interesse
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) { // Prepara la query di inserimento
                insertStmt.setString(1, nome); // Imposta il nome
                insertStmt.setString(2, nomeASCII); // Imposta il nome ASCII
                insertStmt.setString(3, stato); // Imposta lo stato
                insertStmt.setString(4, statoCodice); // Imposta il codice dello stato
                insertStmt.setDouble(5, latitudine); // Imposta la latitudine
                insertStmt.setDouble(6, longitudine); // Imposta la longitudine

                int rowsInserted = insertStmt.executeUpdate(); // Esegue l'inserimento
                return rowsInserted > 0; // Ritorna true se l'inserimento ha successo
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la registrazione dell'area di interesse", e); // Propaga l'eccezione come RemoteException
        }
    }

    /**
     * Recupera le aree geografiche associate a un operatore specifico.
     * @param username Nome utente dell'operatore.
     * @return Lista di mappe contenenti ID e nome ASCII delle aree associate.
     * @throws RemoteException In caso di errore durante la comunicazione con il database.
     */
    @Override
    public List<Map<String, String>> getLocationsForUser(String username) throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>(); // Crea una lista per memorizzare i risultati
        String query = "SELECT coordinatemonitoraggio.id_luogo, coordinatemonitoraggio.nome_ascii " +
                "FROM coordinatemonitoraggio " +
                "INNER JOIN centroarea ON coordinatemonitoraggio.id_luogo = centroarea.id_luogo " +
                "INNER JOIN operatoriregistrati ON centroarea.id_centromonitoraggio = operatoriregistrati.id_centromonitoraggio " +
                "WHERE operatoriregistrati.username = ? " +
                "ORDER BY coordinatemonitoraggio.nome_ascii ASC";

        System.out.println("**DEBUG** Esecuzione della query di getLocationsForUser"); // Debug: indica l'esecuzione della query
        try (Connection connection = dbConnection(); // Crea una connessione al database
             PreparedStatement stmt = connection.prepareStatement(query)) { // Prepara la query SQL
            stmt.setString(1, username); // Imposta il parametro username
            try (ResultSet rs = stmt.executeQuery()) { // Esegue la query
                while (rs.next()) { // Itera sui risultati della query
                    Map<String, String> row = new HashMap<>(); // Crea una mappa per i dati della riga
                    row.put("id_luogo", String.valueOf(rs.getInt("id_luogo"))); // ID del luogo
                    row.put("nome_ascii", rs.getString("nome_ascii")); // Nome ASCII
                    results.add(row); // Aggiunge la mappa ai risultati
                }
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero delle aree per l'utente", e); // Propaga l'errore come RemoteException
        }

        return results; // Ritorna i risultati della ricerca
    }

    /**
     * Aggiunge parametri climatici per un'area specifica.
     * @param username Nome utente dell'operatore che inserisce i parametri.
     * @param nomeArea Nome dell'area di interesse.
     * @param data Data della rilevazione nel formato dd/MM/yyyy.
     * @param ora Ora della rilevazione nel formato HH:mm.
     * @param vento Valore del vento.
     * @param umidita Valore dell'umidità.
     * @param pressione Valore della pressione.
     * @param temperatura Valore della temperatura.
     * @param precipitazioni Valore delle precipitazioni.
     * @param altitudine Valore dell'altitudine dei ghiacciai.
     * @param massa Valore della massa dei ghiacciai.
     * @param commentoVento Commento sul vento.
     * @param commentoUmidita Commento sull'umidità.
     * @param commentoPressione Commento sulla pressione.
     * @param commentoTemperatura Commento sulla temperatura.
     * @param commentoPrecipitazioni Commento sulle precipitazioni.
     * @param commentoAltitudine Commento sull'altitudine dei ghiacciai.
     * @param commentoMassa Commento sulla massa dei ghiacciai.
     * @return True se i parametri sono stati inseriti con successo, false altrimenti.
     * @throws RemoteException In caso di errore durante la comunicazione o l'operazione sul database.
     */
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

        try (Connection connection = dbConnection(); // Stabilisce la connessione con il database
             PreparedStatement stmt = connection.prepareStatement(query)) { // Prepara la query di inserimento

            System.out.println("**DEBUG** Preparazione della query"); // Debug: conferma la preparazione della query

            // Recupera l'ID del centro di monitoraggio associato all'operatore
            String centroQuery = "SELECT id_centromonitoraggio FROM operatoriregistrati WHERE username = ?";
            int idCentroMonitoraggio;

            try (PreparedStatement centroStmt = connection.prepareStatement(centroQuery)) { // Query per ottenere l'ID del centro
                centroStmt.setString(1, username); // Imposta il nome utente
                ResultSet rs = centroStmt.executeQuery(); // Esegue la query
                if (rs.next()) {
                    idCentroMonitoraggio = rs.getInt("id_centromonitoraggio"); // Ottiene l'ID
                    System.out.println("ID Centro Monitoraggio: " + idCentroMonitoraggio); // Debug
                } else {
                    throw new RemoteException("Centro di monitoraggio non trovato per l'utente: " + username); // Errore
                }
            }

            // Recupera l'ID del luogo dall'area
            String luogoQuery = "SELECT id_luogo FROM coordinatemonitoraggio WHERE nome_ascii = ?";
            int idLuogo;

            try (PreparedStatement luogoStmt = connection.prepareStatement(luogoQuery)) { // Query per ottenere l'ID del luogo
                luogoStmt.setString(1, nomeArea); // Imposta il nome dell'area
                ResultSet rs = luogoStmt.executeQuery(); // Esegue la query
                if (rs.next()) {
                    idLuogo = rs.getInt("id_luogo"); // Ottiene l'ID
                    System.out.println("ID Luogo: " + idLuogo); // Debug
                } else {
                    throw new RemoteException("Luogo non trovato per l'area: " + nomeArea); // Errore
                }
            }

            // Conversione della data e dell'ora
            String[] dateParts = data.split("/");
            String formattedDate = String.format("%s-%s-%s", dateParts[2], dateParts[1], dateParts[0]); // yyyy-MM-dd
            String formattedTime = ora + ":00"; // hh:mm:ss
            System.out.println("Formatted Date: " + formattedDate); // Debug: data formattata
            System.out.println("Formatted Time: " + formattedTime); // Debug: ora formattata

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

            System.out.println("**DEBUG** Esecuzione della query di insert"); // Debug: query in esecuzione
            int rowsInserted = stmt.executeUpdate(); // Esegue l'inserimento
            System.out.println("**DEBUG** Rows inserted: " + rowsInserted); // Debug: righe inserite
            return rowsInserted > 0; // Ritorna true se l'inserimento ha successo
        } catch (SQLException e) {
            System.out.println("**DEBUG** SQLException: " + e.getMessage()); // Debug: errore SQL
            e.printStackTrace();
            throw new RemoteException("Errore durante l'inserimento dei parametri climatici", e); // Propaga l'errore
        } catch (IllegalArgumentException e) {
            System.out.println("**DEBUG** IllegalArgumentException: " + e.getMessage()); // Debug: errore argomento
            throw new RemoteException("Errore nel formato della data o dell'ora", e); // Propaga l'errore
        }
    }

    /**
     * Verifica se esiste già un parametro climatico per una specifica area, data e ora.
     * @param nomeArea Nome dell'area di interesse.
     * @param data Data della rilevazione nel formato dd/MM/yyyy.
     * @param ora Ora della rilevazione nel formato HH:mm.
     * @return True se il parametro esiste già, false altrimenti.
     * @throws RemoteException In caso di errore durante la comunicazione o l'operazione sul database.
     */
    @Override
    public boolean checkExistingClimaticParameter(String nomeArea, String data, String ora) throws RemoteException {
        String query = "SELECT COUNT(*) FROM parametriclimatici WHERE id_luogo = (SELECT id_luogo FROM coordinatemonitoraggio WHERE nome_ascii = ?) AND data_di_rilevazione = ? AND ora = ?";

        try (Connection connection = dbConnection(); // Connessione al database
             PreparedStatement stmt = connection.prepareStatement(query)) { // Prepara la query SQL

            // Converti la data dal formato dd/MM/yyyy a yyyy-MM-dd
            String[] dateParts = data.split("/");
            String formattedDate = String.format("%s-%s-%s", dateParts[2], dateParts[1], dateParts[0]); // yyyy-MM-dd
            stmt.setString(1, nomeArea); // Imposta il nome dell'area
            stmt.setDate(2, java.sql.Date.valueOf(formattedDate)); // Imposta la data formattata
            stmt.setTime(3, java.sql.Time.valueOf(ora + ":00")); // Imposta l'ora formattata con ":00"

            ResultSet rs = stmt.executeQuery(); // Esegue la query
            if (rs.next()) {
                return rs.getInt(1) > 0; // Ritorna true se esiste già
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la verifica del parametro climatico esistente.", e); // Propaga l'errore come RemoteException
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            throw new RemoteException("Errore nel formato della data o dell'ora fornita.", e); // Propaga l'errore per formato errato
        }
        return false; // Ritorna false se non esiste
    }

    /**
     * Recupera i dati climatici per un'area specifica tramite il suo ID.
     * @param areaId ID dell'area di interesse.
     * @return Lista di mappe contenenti i dettagli dei dati climatici.
     * @throws RemoteException In caso di errore durante la comunicazione o l'operazione sul database.
     */
    @Override
    public List<Map<String, String>> getClimaticDataById(int areaId) throws RemoteException {
        List<Map<String, String>> results = new ArrayList<>(); // Crea una lista per memorizzare i risultati
        String query = "SELECT id_parametro, data_di_rilevazione, ora, vento, umidita, pressione, " +
                "temperatura, precipitazioni, altitudineghiacciai, massaghiacciai " +
                "FROM parametriclimatici WHERE id_luogo = ? ORDER BY data_di_rilevazione DESC, ora DESC";

        try (Connection connection = dbConnection(); // Connessione al database
             PreparedStatement stmt = connection.prepareStatement(query)) { // Prepara la query SQL

            stmt.setInt(1, areaId); // Imposta l'ID dell'area
            ResultSet rs = stmt.executeQuery(); // Esegue la query

            while (rs.next()) { // Itera sui risultati della query
                Map<String, String> row = new HashMap<>(); // Mappa per memorizzare i dati della riga
                row.put("id_parametro", String.valueOf(rs.getInt("id_parametro"))); // ID del parametro
                row.put("data_di_rilevazione", rs.getDate("data_di_rilevazione").toString()); // Data della rilevazione
                row.put("ora", rs.getTime("ora").toString()); // Ora della rilevazione
                row.put("vento", String.valueOf(rs.getInt("vento"))); // Valore del vento
                row.put("umidita", String.valueOf(rs.getInt("umidita"))); // Valore dell'umidità
                row.put("pressione", String.valueOf(rs.getInt("pressione"))); // Valore della pressione
                row.put("temperatura", String.valueOf(rs.getInt("temperatura"))); // Valore della temperatura
                row.put("precipitazioni", String.valueOf(rs.getInt("precipitazioni"))); // Valore delle precipitazioni
                row.put("altitudineghiacciai", String.valueOf(rs.getInt("altitudineghiacciai"))); // Altitudine dei ghiacciai
                row.put("massaghiacciai", String.valueOf(rs.getInt("massaghiacciai"))); // Massa dei ghiacciai
                System.out.println("DEBUG (Server): ID Parametro = " + row.get("id_parametro")); // Debug per monitorare i dati
                results.add(row); // Aggiunge la mappa ai risultati
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante il recupero dei dati climatici", e); // Propaga l'errore come RemoteException
        }

        return results; // Ritorna i risultati della query
    }

    /**
     * Calcola le medie dei parametri climatici per un'area specifica.
     * @param areaId ID dell'area di interesse.
     * @return Mappa contenente i valori medi dei parametri climatici.
     * @throws RemoteException In caso di errore durante la comunicazione o l'operazione sul database.
     */
    @Override
    public Map<String, Double> getAveragesById(int areaId) throws RemoteException {
        Map<String, Double> averages = new HashMap<>(); // Mappa per memorizzare le medie calcolate
        String query = "SELECT AVG(vento) AS vento_media, AVG(umidita) AS umidita_media, " +
                "AVG(pressione) AS pressione_media, AVG(temperatura) AS temperatura_media, " +
                "AVG(precipitazioni) AS precipitazioni_media, AVG(altitudineghiacciai) AS altitudine_media, " +
                "AVG(massaghiacciai) AS massa_media FROM parametriclimatici WHERE id_luogo = ?";

        try (Connection connection = dbConnection(); // Connessione al database
             PreparedStatement stmt = connection.prepareStatement(query)) { // Prepara la query SQL

            stmt.setInt(1, areaId); // Imposta l'ID dell'area
            ResultSet rs = stmt.executeQuery(); // Esegue la query

            if (rs.next()) { // Verifica se ci sono risultati
                averages.put("vento", rs.getDouble("vento_media")); // Media del vento
                averages.put("umidita", rs.getDouble("umidita_media")); // Media dell'umidità
                averages.put("pressione", rs.getDouble("pressione_media")); // Media della pressione
                averages.put("temperatura", rs.getDouble("temperatura_media")); // Media della temperatura
                averages.put("precipitazioni", rs.getDouble("precipitazioni_media")); // Media delle precipitazioni
                averages.put("altitudineghiacciai", rs.getDouble("altitudine_media")); // Media dell'altitudine dei ghiacciai
                averages.put("massaghiacciai", rs.getDouble("massa_media")); // Media della massa dei ghiacciai
                System.out.println("DEBUG: Media calcolata = " + averages); // Debug per monitorare i valori calcolati
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Stampa dettagli dell'errore per il debug
            throw new RemoteException("Errore durante il calcolo delle medie climatiche", e); // Propaga l'errore come RemoteException
        }

        return averages; // Ritorna la mappa delle medie
    }

    /**
     * Calcola le mode dei parametri climatici per un'area specifica.
     * @param areaId ID dell'area di interesse.
     * @return Mappa contenente i valori modali dei parametri climatici.
     * @throws RemoteException In caso di errore durante la comunicazione o l'operazione sul database.
     */
    @Override
    public Map<String, Integer> getModesById(int areaId) throws RemoteException {
        Map<String, Integer> modes = new HashMap<>(); // Mappa per memorizzare le mode calcolate
        String[] parameters = {"vento", "umidita", "pressione", "temperatura", "precipitazioni", "altitudineghiacciai", "massaghiacciai"};

        try (Connection connection = dbConnection()) { // Connessione al database
            for (String parameter : parameters) { // Itera sui parametri climatici
                String query = "SELECT " + parameter + ", COUNT(" + parameter + ") AS occorrenze " +
                        "FROM parametriclimatici WHERE id_luogo = ? " +
                        "GROUP BY " + parameter + " " +
                        "ORDER BY occorrenze DESC, " + parameter + " ASC LIMIT 1";

                try (PreparedStatement stmt = connection.prepareStatement(query)) { // Prepara la query SQL
                    stmt.setInt(1, areaId); // Imposta l'ID dell'area
                    ResultSet rs = stmt.executeQuery(); // Esegue la query
                    if (rs.next()) { // Verifica se ci sono risultati
                        modes.put(parameter, rs.getInt(parameter)); // Aggiunge la moda del parametro alla mappa
                    } else {
                        modes.put(parameter, 0); // Default se non ci sono dati
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Stampa dettagli dell'errore per il debug
            throw new RemoteException("Errore durante il calcolo delle mode climatiche", e); // Propaga l'errore come RemoteException
        }

        System.out.println("DEBUG: Modes calcolate = " + modes); // Debug per monitorare i valori calcolati
        return modes; // Ritorna la mappa delle mode
    }

    /**
     * Calcola le mediane dei parametri climatici per un'area specifica.
     * @param areaId ID dell'area di interesse.
     * @return Mappa contenente i valori mediani dei parametri climatici.
     * @throws RemoteException In caso di errore durante la comunicazione o l'operazione sul database.
     */
    @Override
    public Map<String, Double> getMediansById(int areaId) throws RemoteException {
        Map<String, Double> medians = new HashMap<>(); // Mappa per memorizzare le mediane calcolate
        String[] parameters = {"vento", "umidita", "pressione", "temperatura", "precipitazioni", "altitudineghiacciai", "massaghiacciai"};

        try (Connection connection = dbConnection()) { // Connessione al database
            for (String parameter : parameters) { // Itera sui parametri climatici
                String query = "SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY " + parameter + ") AS mediana " +
                        "FROM parametriclimatici WHERE id_luogo = ?";

                try (PreparedStatement stmt = connection.prepareStatement(query)) { // Prepara la query SQL
                    stmt.setInt(1, areaId); // Imposta l'ID dell'area
                    ResultSet rs = stmt.executeQuery(); // Esegue la query
                    if (rs.next()) { // Verifica se ci sono risultati
                        medians.put(parameter, rs.getDouble("mediana")); // Aggiunge la mediana del parametro alla mappa
                    } else {
                        medians.put(parameter, 0.0); // Default se non ci sono dati
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Stampa dettagli dell'errore per il debug
            throw new RemoteException("Errore durante il calcolo delle mediane climatiche", e); // Propaga l'errore come RemoteException
        }

        System.out.println("DEBUG: Medians calcolate = " + medians); // Debug per monitorare i valori calcolati
        return medians; // Ritorna la mappa delle mediane
    }

    /**
     * Recupera il commento associato a un parametro climatico specifico.
     * @param idParametro ID del parametro climatico.
     * @param parameterNoteColumn Nome della colonna che contiene il commento del parametro.
     * @return Il commento associato al parametro, oppure "Nessun commento disponibile" se assente.
     * @throws RemoteException In caso di errore durante la comunicazione o l'operazione sul database.
     */
    @Override
    public String getCommentForParameterById(int idParametro, String parameterNoteColumn) throws RemoteException {
        String query = "SELECT " + parameterNoteColumn + " AS commento " +
                "FROM parametriclimatici " +
                "WHERE id_parametro = ?";

        try (Connection connection = dbConnection(); // Connessione al database
             PreparedStatement stmt = connection.prepareStatement(query)) { // Prepara la query SQL

            stmt.setInt(1, idParametro); // Imposta l'ID del parametro come filtro
            ResultSet rs = stmt.executeQuery(); // Esegue la query

            if (rs.next()) { // Controlla se ci sono risultati
                return rs.getString("commento") != null ? rs.getString("commento") : "Nessun commento disponibile."; // Ritorna il commento o un valore di default
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Stampa dettagli dell'errore per il debug
            throw new RemoteException("Errore durante il recupero del commento", e); // Propaga l'errore come RemoteException
        }
        return "Nessun commento disponibile."; // Ritorna il valore di default se non ci sono risultati
    }

    /**
     * Modifica il centro di monitoraggio di un determinato operatore.
     * @param username username del operatore a cui verrà modificato il centro di monitoraggio.
     * @param nomeCentroMonitoraggio Nome del centro di monitoraggio che verrà messo all'operatore.
     * @return True se il centro di monitoraggio è stato aggiornato con successo, false altrimenti.
     * @throws RemoteException In caso di errore durante la comunicazione o l'operazione sul database.
     */
    @Override
    public boolean updateCentroMonitoraggioOperatore(String username, String nomeCentroMonitoraggio) throws RemoteException {
        String query = "UPDATE operatoriregistrati SET id_centromonitoraggio = " +
                "(SELECT id_centromonitoraggio FROM centrimonitoraggio WHERE nome = ?) " +
                "WHERE username = ?";

        try (Connection connection = dbConnection(); // Ottiene la connessione al database
             PreparedStatement stmt = connection.prepareStatement(query)) { // Prepara la query SQL

            // Imposta i parametri della query
            stmt.setString(1, nomeCentroMonitoraggio); // Nome del centro di monitoraggio
            stmt.setString(2, username); // Username dell'operatore

            int rowsUpdated = stmt.executeUpdate(); // Esegue l'aggiornamento
            return rowsUpdated > 0; // Ritorna true se almeno una riga è stata aggiornata

        } catch (SQLException e) {
            throw new RemoteException("Errore durante l'aggiornamento del centro di monitoraggio: " + e.getMessage(), e);
        }
    }

    /**
     * Metodo principale per avviare l'applicazione server.
     * Imposta il tema grafico e inizializza la GUI.
     * @param args Argomenti della riga di comando (non utilizzati).
     */
    public static void main(String[] args) {
        try {
            // Imposta il tema FlatDarkLaf per l'interfaccia grafica
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace(); // Stampa dettagli dell'errore per il debug
            JOptionPane.showMessageDialog(null, "Errore durante l'impostazione del tema: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }

        // Esegui l'inizializzazione della GUI su un thread separato
        SwingUtilities.invokeLater(() -> {
            try {
                new serverCM(); // Avvia la finestra principale del server
            } catch (Exception e) {
                e.printStackTrace(); // Stampa dettagli dell'errore per il debug
                System.err.println("Errore durante l'avvio dell'applicazione: " + e.getMessage());
            }
        });
    }
}