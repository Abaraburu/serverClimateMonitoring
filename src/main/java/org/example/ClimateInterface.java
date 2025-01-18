package org.example;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Interfaccia per il servizio di monitoraggio climatico.
 * Contiene metodi per la gestione delle comunicazioni tra applicazione client e applicazione server.
 *
 * @author Agliati Lorenzo 753378
 */
public interface ClimateInterface extends Remote {
    /**
     * Ottiene tutti i dati delle aree monitorate.
     * @return Lista di mappe con ID e nome delle aree.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    List<Map<String, String>> getAllData() throws RemoteException;

    /**
     * Cerca le aree in base alle coordinate e al raggio.
     * @param latitude Latitudine.
     * @param longitude Longitudine.
     * @param radius Raggio in km.
     * @return Lista di mappe con i dettagli delle aree.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    List<Map<String, String>> searchByCoordinates(double latitude, double longitude, double radius) throws RemoteException;

    /**
     * Cerca le aree in base al nome.
     * @param name Nome da cercare.
     * @return Lista di mappe con i dettagli delle aree.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    List<Map<String, String>> searchByName(String name) throws RemoteException;

    /**
     * Ottiene i dati minimali delle aree monitorate.
     * @return Lista di mappe con ID e nome ASCII delle aree.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    List<Map<String, String>> getMinimalLocationData() throws RemoteException;

    /**
     * Valida le credenziali di un utente.
     * @param userId ID utente.
     * @param password Password utente.
     * @return True se le credenziali sono valide, false altrimenti.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    boolean validateCredentials(String userId, String password) throws RemoteException;

    /**
     * Registra un nuovo centro di monitoraggio.
     * @param name Nome del centro di monitoraggio.
     * @param address Indirizzo del centro.
     * @param areaIds Lista degli ID delle aree associate.
     * @param isCheckboxChecked Indica se la checkbox è selezionata.
     * @param username Nome utente dell'operatore associato.
     * @return True se la registrazione ha successo, false altrimenti.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    boolean registerMonitoringCenter(String name, String address, List<Integer> areaIds, boolean isCheckboxChecked, String username) throws RemoteException;

    /**
     * Controlla se esiste un centro di monitoraggio con nome o indirizzo duplicato.
     * @param name Nome del centro.
     * @param address Indirizzo del centro.
     * @return True se esiste un duplicato, false altrimenti.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    boolean checkDuplicateMonitoringCenter(String name, String address) throws RemoteException;

    /**
     * Ottiene la lista di tutti i centri di monitoraggio.
     * @return Lista di nomi dei centri.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    List<String> getAllMonitoringCenters() throws RemoteException;

    /**
     * Registra un nuovo operatore.
     * @param nome Nome dell'operatore.
     * @param cognome Cognome dell'operatore.
     * @param codiceFiscale Codice fiscale dell'operatore.
     * @param email Email dell'operatore.
     * @param username Username dell'operatore.
     * @param password Password dell'operatore.
     * @param centroMonitoraggio Centro di monitoraggio associato.
     * @return True se la registrazione ha successo, false altrimenti.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    boolean registerOperator(String nome, String cognome, String codiceFiscale, String email, String username, String password, String centroMonitoraggio) throws RemoteException;

    /**
     * Registra una nuova area di interesse.
     * @param nome Nome dell'area.
     * @param nomeASCII Nome ASCII dell'area.
     * @param stato Stato dell'area.
     * @param statoCodice Codice dello stato.
     * @param latitudine Latitudine dell'area.
     * @param longitudine Longitudine dell'area.
     * @return True se la registrazione ha successo, false altrimenti.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    boolean registerArea(String nome, String nomeASCII, String stato, String statoCodice, double latitudine, double longitudine) throws RemoteException;

    /**
     * Ottiene le aree associate a un utente.
     * @param username Username dell'utente.
     * @return Lista di mappe con i dettagli delle aree.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    List<Map<String, String>> getLocationsForUser(String username) throws RemoteException;

    /**
     * Aggiunge parametri climatici per un'area.
     * @param username Username dell'utente.
     * @param nomeArea Nome dell'area.
     * @param data Data della rilevazione.
     * @param ora Ora della rilevazione.
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
     * @param commentoAltitudine Commento sull'altitudine.
     * @param commentoMassa Commento sulla massa.
     * @return True se l'aggiunta ha successo, false altrimenti.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    boolean addClimaticParameters(String username, String nomeArea, String data, String ora, int vento, int umidita, int pressione, int temperatura, int precipitazioni, int altitudine, int massa, String commentoVento, String commentoUmidita, String commentoPressione, String commentoTemperatura, String commentoPrecipitazioni, String commentoAltitudine, String commentoMassa) throws RemoteException;

    /**
     * Controlla se esistono già parametri climatici per lo stesso luogo, data e ora.
     * @param nomeArea Nome dell'area.
     * @param data Data della rilevazione.
     * @param ora Ora della rilevazione.
     * @return True se esistono già, false altrimenti.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    boolean checkExistingClimaticParameter(String nomeArea, String data, String ora) throws RemoteException;

    /**
     * Ottiene dati climatici tramite ID dell'area.
     * @param areaId ID dell'area.
     * @return Lista di mappe con i dettagli dei dati climatici.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    List<Map<String, String>> getClimaticDataById(int areaId) throws RemoteException;

    /**
     * Calcola le medie climatiche tramite ID dell'area.
     * @param areaId ID dell'area.
     * @return Mappa con le medie dei parametri climatici.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    Map<String, Double> getAveragesById(int areaId) throws RemoteException;

    /**
     * Calcola le mode climatiche tramite ID dell'area.
     * @param areaId ID dell'area.
     * @return Mappa con le mode dei parametri climatici.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    Map<String, Integer> getModesById(int areaId) throws RemoteException;

    /**
     * Calcola le mediane climatiche tramite ID dell'area.
     * @param areaId ID dell'area.
     * @return Mappa con le mediane dei parametri climatici.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    Map<String, Double> getMediansById(int areaId) throws RemoteException;

    /**
     * Ottiene il commento di un parametro climatico tramite ID del parametro.
     * @param idParametro ID del parametro.
     * @param parameterNoteColumn Nome della colonna per il commento.
     * @return Commento associato al parametro.
     * @throws RemoteException In caso di errore di comunicazione.
     */
    String getCommentForParameterById(int idParametro, String parameterNoteColumn) throws RemoteException;
}