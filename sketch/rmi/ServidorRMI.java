package rmi;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class ServidorRMI {

    public static void main(String args[]) {
        try {
            // 1. Crear e iniciar el rmiregistry en el puerto 1099
            LocateRegistry.createRegistry(1099);
            System.out.println("RMI registry iniciado.");

            // 2. Instanciar el objeto servidor
            SubastaModeloRMI modelo = new SubastaModeloRMI();

            // 3. Registrar el objeto en el registry
            // [cite: 62]
            Naming.rebind("//localhost/SubastaServidor", modelo);

            System.out.println("Servidor de Subasta RMI listo.");

        } catch (Exception e) {
            System.err.println("Excepción en el ServidorRMI: " + e.toString());
            e.printStackTrace();
        }
    }
}