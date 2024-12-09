package org.example;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface ClimateInterface extends Remote {
    List<Map<String, String>> getAllData() throws RemoteException;
}
