package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClienteCallbackRMI extends Remote {

    // El servidor llamará a este método para notificar al cliente de una
    // actualización
    void notificarActualizacion(String producto, float nuevoPrecio) throws RemoteException;
}