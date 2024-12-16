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
}