package rmi;

import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;

public class ClienteRMI {

    // Método para forzar la actualización de la vista
    public static void forzarActualizacionVista(SubastaControladorRMI controlador) {
        System.out.println("-> Notificación recibida. Forzando actualización de la lista.");
        controlador.actualizarCatalogo();
    }

    public static void main(String args[]) {

        String host = (args.length < 1) ? "localhost" : args[0]; // [cite: 71]

        try {
            // 1. Localizar el servidor remoto
            // [cite: 65]
            String url = "//" + host + "/SubastaServidor";
            SubastaServidorRMI servidor = (SubastaServidorRMI) Naming.lookup(url);

            // 2. Configurar el MVC
            SubastaVista vista = new SubastaVista();
            SubastaControladorRMI controlador = new SubastaControladorRMI(vista, servidor); // Pasa el stub del servidor

            vista.asignarActionListener(controlador);
            vista.asignarListSelectionListener(controlador);

            // 3. Configurar y registrar el callback
            ClienteCallbackImpl callback = new ClienteCallbackImpl(controlador);

            // Exportar el objeto callback para que el servidor pueda invocarlo
            ClienteCallbackRMI callbackStub = (ClienteCallbackRMI) UnicastRemoteObject.exportObject(callback, 0);

            servidor.registrarClienteCallback(callbackStub);

            System.out.println("Cliente RMI conectado y callback registrado.");

            // Opcional: Asegurarse de eliminar el callback al cerrar la ventana
            // (requeriría añadir un WindowListener a SubastaVista)

        } catch (Exception e) {
            System.err.println("Excepción en el ClienteRMI: " + e.toString());
            e.printStackTrace();
        }
    }
}