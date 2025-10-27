package grpcapp;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

// Importaciones de gRPC
import io.grpc.subasta.*;

public class SubastaControladorGPRC implements ActionListener, ListSelectionListener {

    SubastaVista vista;
    // Stub bloqueante para las acciones del usuario
    SubastaServicioGrpc.SubastaServicioBlockingStub blockingStub;
    Hashtable<String, String> listaConPrecios; // Sigue siendo útil para la GUI

    public SubastaControladorGPRC(SubastaVista v, SubastaServicioGrpc.SubastaServicioBlockingStub stub) {
        vista = v;
        blockingStub = stub;
    }

    public void actionPerformed(ActionEvent evento) {
        String usuario;
        String producto;
        float monto;

        System.out.println("<<" + evento.getActionCommand() + ">>");

        try {
            if (evento.getActionCommand().equals("Salir")) {
                System.exit(1);
            } else if (evento.getActionCommand().equals("Conectar")) {
                usuario = vista.getUsuario();
                System.out.println("Registrarse como usuario: " + usuario);
                UsuarioRequest req = UsuarioRequest.newBuilder().setNombre(usuario).build();
                blockingStub.registraUsuario(req); // Llamada gRPC

            } else if (evento.getActionCommand().equals("Poner a la venta")) {
                usuario = vista.getUsuario();
                producto = vista.getProducto();
                monto = vista.getPrecioInicial();
                System.out.println("Poniendo a la venta: " + producto);
                ProductoRequest req = ProductoRequest.newBuilder()
                        .setVendedor(usuario)
                        .setProducto(producto)
                        .setPrecioInicial(monto)
                        .build();
                blockingStub.agregaProductoALaVenta(req); // Llamada gRPC

            } else if (evento.getActionCommand().equals("Obtener lista")) {
                actualizarCatalogo(); // Llama al método refactorizado

            } else if (evento.getActionCommand().equals("Ofrecer")) {
                producto = vista.getProductoSeleccionado();
                monto = vista.getMontoOfrecido();
                usuario = vista.getUsuario();
                System.out.println("Ofreciendo " + monto + " por " + producto);
                OfertaRequest req = OfertaRequest.newBuilder()
                        .setComprador(usuario)
                        .setProducto(producto)
                        .setMonto(monto)
                        .build();
                blockingStub.agregaOferta(req); // Llamada gRPC
            }
        } catch (Exception e) {
            System.err.println("Error de gRPC en el controlador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para actualizar la lista (llamado por el botón o por el stream)
    public void actualizarCatalogo() {
        System.out.println("Actualizando catálogo desde el servidor gRPC...");
        try {
            CatalogoRespuesta respuesta = blockingStub.obtieneCatalogo(Empty.newBuilder().build());

            listaConPrecios = new Hashtable<>();
            vista.reinicializaListaProductos();

            for (ProductoInfo info : respuesta.getProductosList()) {
                listaConPrecios.put(info.getProducto(), String.valueOf(info.getPrecioActual()));
                vista.agregaProducto(info.getProducto());
            }
        } catch (Exception e) {
            System.err.println("Error gRPC al obtener catálogo: " + e.getMessage());
        }
    }

    // Método para actualizar el precio en la GUI (llamado por el stream)
    public void actualizarPrecioSiSeleccionado(String producto, float nuevoPrecio) {
        String seleccionado = vista.getProductoSeleccionado();
        if (seleccionado != null && seleccionado.equals(producto)) {
            vista.desplegarPrecio(String.valueOf(nuevoPrecio));
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
