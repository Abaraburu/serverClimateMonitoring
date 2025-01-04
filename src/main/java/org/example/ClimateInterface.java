package org.example;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface ClimateInterface extends Remote {
    // Metodo per ottenere tutti i dati
    List<Map<String, String>> getAllData() throws RemoteException;

    // Metodo per cercare con coordinate e raggio
    List<Map<String, String>> searchByCoordinates(double latitude, double longitude, double radius) throws RemoteException;

    // Metodo per la ricerca basata sul nome
    List<Map<String, String>> searchByName(String name) throws RemoteException;

    // Metodo per ottenere solo ID_Luogo e Nome ASCII
    List<Map<String, String>> getMinimalLocationData() throws RemoteException;

    // Metodo per validare credenziali
    boolean validateCredentials(String userId, String password) throws RemoteException;

    // Metodo per registrare un centro di monitoraggio
    boolean registerMonitoringCenter(String name, String address, List<Integer> areaIds) throws RemoteException;

    // Metodo per il controllo nell aggiunta del centro di monitoraggio
    boolean checkDuplicateMonitoringCenter(String name, String address) throws RemoteException;

    // Metodo per ottenere tutti i centri di monitoraggio
    List<String> getAllMonitoringCenters() throws RemoteException;

    // Metodo per registrare un operatore con controlli su duplicati
    boolean registerOperator(String nome, String cognome, String codiceFiscale, String email, String username, String password, String centroMonitoraggio) throws RemoteException;

    // Metodo per registrare un'area di interesse
    boolean registerArea(String nome, String nomeASCII, String stato, String statoCodice, double latitudine, double longitudine) throws RemoteException;
}