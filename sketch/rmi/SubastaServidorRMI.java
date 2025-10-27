package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

public interface SubastaServidorRMI extends Remote {

    boolean registraUsuario(String nombre) throws RemoteException;

    boolean agregaProductoALaVenta(String vendedor, String producto, float precioInicial) throws RemoteException;

    boolean agregaOferta(String comprador, String producto, float monto) throws RemoteException;

    Vector<InformacionProducto> obtieneCatalogo() throws RemoteException;

    // Métodos para el callback (la solución de sincronización)
    void registrarClienteCallback(ClienteCallbackRMI cliente) throws RemoteException;

    void eliminarClienteCallback(ClienteCallbackRMI cliente) throws RemoteException;
}