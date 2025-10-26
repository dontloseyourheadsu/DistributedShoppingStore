package rmi;

import java.rmi.RemoteException;

public class ClienteCallbackImpl implements ClienteCallbackRMI {

    private SubastaControladorRMI controlador;

    public ClienteCallbackImpl(SubastaControladorRMI controlador) throws RemoteException {
        super();
        this.controlador = controlador;
    }

    // Esta es la función que el servidor invoca
    @Override
    public void notificarActualizacion(String producto, float nuevoPrecio) throws RemoteException {
        System.out.println("CALLBACK RECIBIDO: El precio de '" + producto + "' ahora es " + nuevoPrecio);

        // Llama al controlador para que actualice la vista
        // Esto resuelve el problema de sincronización
        controlador.actualizarCatalogo();

        // Opcional: actualizar el precio del item seleccionado si coincide
        controlador.actualizarPrecioSiSeleccionado(producto, nuevoPrecio);
    }
}