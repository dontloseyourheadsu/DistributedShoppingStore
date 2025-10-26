package rmi;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

public class SubastaModeloRMI extends UnicastRemoteObject implements SubastaServidorRMI {

    Hashtable usuarios;
    Hashtable productos;
    Hashtable ofertas;

    // Lista para los callbacks
    private Vector<ClienteCallbackRMI> clientesConectados;

    public SubastaModeloRMI() throws RemoteException {
        super(); // Llama al constructor de UnicastRemoteObject
        usuarios = new Hashtable();
        productos = new Hashtable();
        ofertas = new Hashtable();
        clientesConectados = new Vector<ClienteCallbackRMI>();
    }

    // --- Implementación de métodos de la interfaz ---

    public synchronized boolean registraUsuario(String nombre) throws RemoteException {
        if (!usuarios.containsKey(nombre)) {
            System.out.println("Agregando un nuevo usuario: " + nombre);
            usuarios.put(nombre, nombre);
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean agregaProductoALaVenta(String vendedor, String producto, float precioInicial)
            throws RemoteException {
        if (!productos.containsKey(producto)) {
            System.out.println("Agregando un nuevo producto: " + producto);
            productos.put(producto, new InformacionProducto(vendedor, producto, precioInicial));

            // Notificar a todos los clientes sobre el *nuevo* producto (podrían querer
            // actualizar)
            // Por simplicidad, usamos la misma notificación genérica.
            notificarATodos("Nuevo producto: " + producto, precioInicial);
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean agregaOferta(String comprador, String producto, float monto) throws RemoteException {
        if (productos.containsKey(producto)) {
            InformacionProducto infoProd;
            infoProd = (InformacionProducto) productos.get(producto);

            if (infoProd.actualizaPrecio(monto)) {
                ofertas.put(producto + comprador, new InformacionOferta(comprador, producto, monto));

                // ¡Punto clave! Notificar a todos los clientes sobre el cambio de precio
                System.out.println("Notificando cambio de precio para: " + producto);
                notificarATodos(producto, monto);

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public synchronized Vector obtieneCatalogo() throws RemoteException {
        Vector resultado;
        resultado = new Vector(productos.values());
        return resultado;
    }

    // --- Implementación de métodos de Callback ---

    public synchronized void registrarClienteCallback(ClienteCallbackRMI cliente) throws RemoteException {
        if (!clientesConectados.contains(cliente)) {
            clientesConectados.add(cliente);
            System.out.println("Nuevo cliente callback registrado.");
        }
    }

    public synchronized void eliminarClienteCallback(ClienteCallbackRMI cliente) throws RemoteException {
        if (clientesConectados.remove(cliente)) {
            System.out.println("Cliente callback eliminado.");
        }
    }

    // --- Lógica de Notificación (La solución) ---

    private synchronized void notificarATodos(String producto, float nuevoPrecio) {
        // Itera sobre los clientes registrados y les envía la notificación
        for (int i = 0; i < clientesConectados.size(); i++) {
            ClienteCallbackRMI cliente = clientesConectados.get(i);
            try {
                cliente.notificarActualizacion(producto, nuevoPrecio);
            } catch (RemoteException e) {
                // Si hay un error (ej. cliente desconectado), lo eliminamos de la lista
                System.out.println("Error notificando a un cliente, eliminando: " + e.getMessage());
                clientesConectados.remove(i);
                i--; // Ajusta el índice tras la eliminación
            }
        }
    }
}