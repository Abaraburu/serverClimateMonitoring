package org.example;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

public class serverStart extends JFrame {

    private JButton startServerButton;
    private JPanel serverStartPanel;
    private JButton stopServerButton;

    // Configurazione del database
    private static final String URL = "jdbc:postgresql://localhost:5432/climatedb";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Asdf1234";

    // Porta del server
    private static final int SERVER_PORT = 8080;

    private boolean isServerRunning = false;

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
                if (!isServerRunning) {
                    System.out.println("Avvio server...");
                    testDatabaseConnection(); // Testa la connessione prima di avviare il server
                    querySpecifica();        // Esegue la query specifica
                    startServer();
                } else {
                    JOptionPane.showMessageDialog(serverStart.this, "Il server è già in esecuzione!");
                }
            }
        });

        stopServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isServerRunning = false;
                JOptionPane.showMessageDialog(serverStart.this, "Server arrestato.");
                System.exit(0); // Puoi aggiungere una logica più sofisticata se necessario
            }
        });
    }

    // Metodo per testare la connessione al database
    private void testDatabaseConnection() {
        System.out.println("Eseguendo test della connessione al database...");
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Connessione al database avvenuta con successo.");
        } catch (SQLException e) {
            System.err.println("Errore nella connessione al database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Metodo per eseguire la query specifica e stampare i risultati
    private void querySpecifica() {
        System.out.println("Eseguendo query specifica...");
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "SELECT * FROM coordinatemonitoraggio WHERE id_luogo = 4946136;";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int idLuogo = rs.getInt("id_luogo");
                    double latitudine = rs.getDouble("latitudine");
                    double longitudine = rs.getDouble("longitudine");

                    System.out.printf("True test connessione DB: ID Luogo: %d, Latitudine: %.6f, Longitudine: %.6f%n",
                            idLuogo, latitudine, longitudine);
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nell'esecuzione della query specifica: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Metodo per avviare il server
    private void startServer() {
        isServerRunning = true;
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
                JOptionPane.showMessageDialog(this, "Server avviato sulla porta " + SERVER_PORT);
                System.out.println("Server in ascolto sulla porta " + SERVER_PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nuovo client connesso: " + clientSocket.getInetAddress());
                    // Gestione del client su un nuovo thread
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Errore nel server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                isServerRunning = false;
            }
        }).start();
    }

    // Classe per gestire i client
    private class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                String request;
                while ((request = in.readLine()) != null) {
                    System.out.println("Richiesta ricevuta: " + request);
                    String response = handleRequest(request);
                    out.println(response);
                }
            } catch (IOException e) {
                System.err.println("Errore nella comunicazione con il client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Errore nella chiusura del socket: " + e.getMessage());
                }
            }
        }

        private String handleRequest(String request) {
            try {
                if (request.startsWith("REGISTRA OPERATORE")) {
                    String[] parts = request.split(";");
                    if (parts.length != 4) {
                        return "Errore: dati insufficienti. Formato: REGISTRA OPERATORE;Nome;Cognome;Email";
                    }
                    return registraOperatore(parts[1], parts[2], parts[3]);
                } else if (request.startsWith("RECUPERA DATI")) {
                    return recuperaDatiClimatici();
                } else {
                    return "Errore: Generale";
                }
            } catch (Exception e) {
                return "Errore nell'elaborazione della richiesta: " + e.getMessage();
            }
        }

        private String registraOperatore(String nome, String cognome, String email) {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "INSERT INTO operatoriregistrati (nome, cognome, email) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, nome);
                    stmt.setString(2, cognome);
                    stmt.setString(3, email);
                    stmt.executeUpdate();
                    return "Operatore registrato con successo!";
                }
            } catch (SQLException e) {
                return "Errore nella registrazione dell'operatore: " + e.getMessage();
            }
        }

        private String recuperaDatiClimatici() {
            StringBuilder response = new StringBuilder();
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT * FROM parametriclimatici";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        response.append("ID: ").append(rs.getInt("id_parametro"))
                                .append(", Luogo: ").append(rs.getInt("id_luogo"))
                                .append(", Temperatura: ").append(rs.getInt("temperatura"))
                                .append(", Umidità: ").append(rs.getInt("umidita"))
                                .append("\n");
                    }
                    return response.toString();
                }
            } catch (SQLException e) {
                return "Errore nel recupero dei dati climatici: " + e.getMessage();
            }
        }
    }

    public static void main(String[] args) {
        new serverStart();
    }
}
