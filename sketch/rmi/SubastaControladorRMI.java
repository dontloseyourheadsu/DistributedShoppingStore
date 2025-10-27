package rmi;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import javax.swing.JList;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.rmi.RemoteException;

public class SubastaControladorRMI implements ActionListener, ListSelectionListener {

    SubastaVista vista;
    SubastaServidorRMI servidor; // Antes era SubastaModelo modelo
    Hashtable<String, String> listaConPrecios;

    public SubastaControladorRMI(SubastaVista v, SubastaServidorRMI s) {
        vista = v;
        servidor = s; // Asigna el stub del servidor
    }

    public void actionPerformed(ActionEvent evento) {
        String usuario;
        String producto;
        float monto;

        System.out.println("<<" + evento.getActionCommand() + ">>");

        try {
            if (evento.getActionCommand().equals("Salir")) {
                // Opcional: De-registrar el callback antes de salir
                // (requeriría guardar el stub del callback)
                System.exit(1);
            } else if (evento.getActionCommand().equals("Conectar")) {
                usuario = vista.getUsuario();
                System.out.println("Registrarse como usuario: " + usuario);
                servidor.registraUsuario(usuario); // Llamada RMI
            } else if (evento.getActionCommand().equals("Poner a la venta")) {
                usuario = vista.getUsuario();
                producto = vista.getProducto();
                monto = vista.getPrecioInicial();
                System.out.println("Poniendo a la venta: " + producto);
                servidor.agregaProductoALaVenta(usuario, producto, monto); // Llamada RMI
            } else if (evento.getActionCommand().equals("Obtener lista")) {
                actualizarCatalogo(); // Llama al método refactorizado
            } else if (evento.getActionCommand().equals("Ofrecer")) {
                producto = vista.getProductoSeleccionado();
                monto = vista.getMontoOfrecido();
                usuario = vista.getUsuario();
                System.out.println("Ofreciendo " + monto + " por " + producto);
                servidor.agregaOferta(usuario, producto, monto); // Llamada RMI
            }
        } catch (RemoteException e) {
            System.err.println("Error de RMI en el controlador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para actualizar la lista (llamado por el botón o por el callback)
    public void actualizarCatalogo() {
        System.out.println("Actualizando catálogo desde el servidor...");
        try {
            Vector<InformacionProducto> lista = servidor.obtieneCatalogo(); // Llamada RMI
            Enumeration<InformacionProducto> it;
            InformacionProducto info;
            listaConPrecios = new Hashtable<String, String>();
            vista.reinicializaListaProductos();
            it = lista.elements();
            while (it.hasMoreElements()) {
                info = it.nextElement();
                listaConPrecios.put(info.producto, String.valueOf(info.precioActual));
                vista.agregaProducto(info.producto);
            }
        } catch (RemoteException e) {
            System.err.println("Error RMI al obtener catálogo: " + e.getMessage());
        }
    }

    // Método para actualizar el precio en la GUI (llamado por el callback)
    public void actualizarPrecioSiSeleccionado(String producto, float nuevoPrecio) {
        String seleccionado = vista.getProductoSeleccionado();
        if (seleccionado != null && seleccionado.equals(producto)) {
            vista.desplegarPrecio(String.valueOf(nuevoPrecio));
            // Actualizar también el precio en nuestro hashtable local
            if (listaConPrecios != null) {
                listaConPrecios.put(producto, String.valueOf(nuevoPrecio));
            }
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == false) {
            String item = vista.getProductoSeleccionado();
            if (item != null) {
                System.out.println(item);
                String precio = listaConPrecios.get(item);
                vista.desplegarPrecio(precio);
            }
        }
    }
}