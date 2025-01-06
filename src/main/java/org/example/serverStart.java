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

/**
 * Classe principale per l'avvio del server di Climate Monitoring.
 * Gestisce la configurazione e l'avvio del server RMI e la connessione al database.
 *
 * @author Agliati Lorenzo 753378
 */
public class serverStart extends JFrame implements ClimateInterface {

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

    /**
     * Nome utente per il database.
     */
    private static final String USER = "postgres";

    /**
     * Password per il database.
     */
    private static final String PASSWORD = "Asdf1234";

    /**
     * Connessione al database.
     */
    private Connection conn;

    /**
     * Indica se il server è in esecuzione.
     */
    private boolean isServerRunning = false;

    /**
     * Costruttore della classe serverStart.
     * Inizializza la GUI e i listener degli eventi.
     */
    public serverStart() {
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
                        JOptionPane.showMessageDialog(serverStart.this, "Inserire un valore valido nel formato IP:PORT.", "Errore", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try {
                        System.out.println("Valore inserito nella textfield: " + ipAndPort); // Debug: mostra il valore
                        startRmiServer(); // Avvia il server RMI
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

        // Listener per fermare il server
        stopServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isServerRunning) {
                    isServerRunning = false; // Cambia lo stato del server
                    status.setText("Status: Server fermo."); // Aggiorna l'etichetta dello stato
                    JOptionPane.showMessageDialog(serverStart.this, "Server arrestato."); // Messaggio di conferma
                    System.exit(0); // Chiude l'applicazione
                } else {
                    JOptionPane.showMessageDialog(serverStart.this, "Il server è già fermo!");
                }
            }
        });
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
        return "jdbc:postgresql://" + ipAndPort + "/climatedb"; // Costruisce l'URL del database
    }

    /**
     * Stabilisce una connessione al database.
     * @return Oggetto Connection per l'accesso al database.
     * @throws SQLException In caso di errore durante la connessione.
     */
    private Connection dbConnection() throws SQLException {
        if (conn == null || conn.isClosed()) { // Verifica se la connessione è chiusa
            String url = getDatabaseUrl(); // Ottiene l'URL del database
            conn = DriverManager.getConnection(url, USER, PASSWORD); // Connette al database
            System.out.println("Connessione al database avvenuta con successo."); // Debug: connessione avvenuta
        }
        return conn; // Ritorna la connessione attiva
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
     * Registra un nuovo centro di monitoraggio e associa aree esistenti.
     * @param name Nome del centro di monitoraggio.
     * @param address Indirizzo del centro di monitoraggio.
     * @param areaIds Lista degli ID delle aree da associare al centro.
     * @return True se la registrazione ha successo, false altrimenti.
     * @throws RemoteException In caso di errore durante la comunicazione o l'operazione sul database.
     */
    @Override
    public boolean registerMonitoringCenter(String name, String address, List<Integer> areaIds) throws RemoteException {
        String insertCenterQuery = "INSERT INTO centrimonitoraggio (nome, indirizzo) VALUES (?, ?) RETURNING id_centromonitoraggio"; // Query per inserire il centro
        String insertAssociationQuery = "INSERT INTO centroarea (id_centromonitoraggio, id_luogo) VALUES (?, ?)"; // Query per associare le aree

        try (Connection connection = dbConnection()) { // Ottiene la connessione al database
            connection.setAutoCommit(false); // Avvia una transazione

            int centerId;
            try (PreparedStatement centerStmt = connection.prepareStatement(insertCenterQuery)) { // Prepara la query di inserimento
                centerStmt.setString(1, name); // Imposta il nome
                centerStmt.setString(2, address); // Imposta l'indirizzo
                ResultSet rs = centerStmt.executeQuery(); // Esegue la query e ottiene l'ID

                if (rs.next()) {
                    centerId = rs.getInt("id_centromonitoraggio"); // Recupera l'ID del centro
                } else {
                    connection.rollback(); // Annulla la transazione
                    throw new SQLException("Errore nel recuperare l'ID del centro di monitoraggio appena inserito.");
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

            connection.commit(); // Conclude la transazione
            return true; // Restituisce successo

        } catch (SQLException e) {
            e.printStackTrace(); // Stampa l'errore per il debug
            throw new RemoteException("Errore durante la registrazione del centro di monitoraggio", e); // Propaga l'eccezione
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
        String checkQuery = "SELECT COUNT(*) FROM operatoriregistrati WHERE codice_fiscale = ? OR email = ? OR username = ?"; // Query per verificare duplicati
        String getIdQuery = "SELECT id_centromonitoraggio FROM centrimonitoraggio WHERE nome = ?"; // Query per ottenere l'ID del centro
        String insertQuery = "INSERT INTO operatoriregistrati (nome, cognome, codice_fiscale, email, username, password, id_centromonitoraggio) VALUES (?, ?, ?, ?, ?, ?, ?)"; // Query per inserire l'operatore

        try (Connection connection = dbConnection()) { // Ottiene la connessione al database
            // Controllo duplicati
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, codiceFiscale); // Imposta il codice fiscale
                checkStmt.setString(2, email); // Imposta l'email
                checkStmt.setString(3, username); // Imposta l'username
                ResultSet rs = checkStmt.executeQuery(); // Esegue la query

                if (rs.next() && rs.getInt(1) > 0) { // Controlla se esistono duplicati
                    throw new RemoteException("Dati duplicati trovati per codice fiscale, email o username."); // Propaga l'errore
                }
            }

            // Ottenere l'ID del centro monitoraggio
            int idCentroMonitoraggio;
            try (PreparedStatement getIdStmt = connection.prepareStatement(getIdQuery)) { // Prepara la query per ottenere l'ID
                getIdStmt.setString(1, centroMonitoraggio); // Imposta il nome del centro
                ResultSet rs = getIdStmt.executeQuery(); // Esegue la query
                if (rs.next()) {
                    idCentroMonitoraggio = rs.getInt("id_centromonitoraggio"); // Ottiene l'ID del centro
                } else {
                    throw new RemoteException("Centro monitoraggio non trovato: " + centroMonitoraggio); // Propaga l'errore se il centro non esiste
                }
            }

            // Inserimento dell'operatore
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) { // Prepara la query di inserimento
                insertStmt.setString(1, nome); // Imposta il nome
                insertStmt.setString(2, cognome); // Imposta il cognome
                insertStmt.setString(3, codiceFiscale); // Imposta il codice fiscale
                insertStmt.setString(4, email); // Imposta l'email
                insertStmt.setString(5, username); // Imposta l'username
                insertStmt.setString(6, password); // Imposta la password
                insertStmt.setInt(7, idCentroMonitoraggio); // Imposta l'ID del centro

                int rowsInserted = insertStmt.executeUpdate(); // Esegue l'inserimento
                return rowsInserted > 0; // Ritorna true se l'inserimento ha successo
            }
        } catch (SQLException e) {
            throw new RemoteException("Errore durante la registrazione dell'operatore", e); // Propaga l'errore come RemoteException
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
                new serverStart(); // Avvia la finestra principale del server
            } catch (Exception e) {
                e.printStackTrace(); // Stampa dettagli dell'errore per il debug
                System.err.println("Errore durante l'avvio dell'applicazione: " + e.getMessage());
            }
        });
    }
}