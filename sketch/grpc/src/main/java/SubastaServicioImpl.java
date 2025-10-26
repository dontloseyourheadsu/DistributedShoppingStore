package grpc.src.main.java;

import io.grpc.stub.StreamObserver;
import io.grpc.subasta.*; // Importa las clases generadas por proto

import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

// Clase interna para almacenar la info del producto (equivalente a InformacionProducto.java)
class Producto {
    String vendedor;
    String nombre;
    float precioActual;

    Producto(String v, String n, float pi) {
        this.vendedor = v;
        this.nombre = n;
        this.precioActual = pi;
    }

    public synchronized boolean actualizaPrecio(float monto) {
        if (monto > precioActual) {
            precioActual = monto;
            return true;
        }
        return false;
    }
}

public class SubastaServicioImpl extends SubastaServicioGrpc.SubastaServicioImplBase {

    // Almacenamiento de datos (versión simplificada de SubastaModelo)
    private Hashtable<String, String> usuarios = new Hashtable<>();
    private ConcurrentHashMap<String, Producto> productos = new ConcurrentHashMap<>();

    // --- Solución de Sincronización ---
    // Almacena los "observadores" de stream para cada cliente conectado.
    // Usamos ConcurrentHashMap para seguridad en hilos.
    private ConcurrentHashMap<String, StreamObserver<NotificacionUpdate>> subscriptores = new ConcurrentHashMap<>();
    private static int subscriberIdCounter = 0;

    @Override
    public void registraUsuario(UsuarioRequest request, StreamObserver<RespuestaBooleana> responseObserver) {
        String nombre = request.getNombre();
        boolean exito = false;
        if (!usuarios.containsKey(nombre)) {
            System.out.println("Agregando un nuevo usuario: " + nombre);
            usuarios.put(nombre, nombre);
            exito = true;
        }
        responseObserver.onNext(RespuestaBooleana.newBuilder().setExito(exito).build());
        responseObserver.onCompleted();
    }

    @Override
    public void agregaProductoALaVenta(ProductoRequest request, StreamObserver<RespuestaBooleana> responseObserver) {
        boolean exito = false;
        if (!productos.containsKey(request.getProducto())) {
            System.out.println("Agregando un nuevo producto: " + request.getProducto());
            Producto p = new Producto(request.getVendedor(), request.getProducto(), request.getPrecioInicial());
            productos.put(request.getProducto(), p);
            exito = true;

            // Notificar a todos los subscriptores
            NotificacionUpdate notificacion = NotificacionUpdate.newBuilder()
                    .setTipo("NUEVO_PRODUCTO")
                    .setProducto(request.getProducto())
                    .setNuevoPrecio(request.getPrecioInicial())
                    .build();
            notificarATodos(notificacion);
        }
        responseObserver.onNext(RespuestaBooleana.newBuilder().setExito(exito).build());
        responseObserver.onCompleted();
    }

    @Override
    public void agregaOferta(OfertaRequest request, StreamObserver<RespuestaBooleana> responseObserver) {
        boolean exito = false;
        Producto p = productos.get(request.getProducto());
        if (p != null) {
            if (p.actualizaPrecio(request.getMonto())) {
                System.out.println("Oferta aceptada para: " + request.getProducto());
                exito = true;

                // Notificar a todos los subscriptores
                NotificacionUpdate notificacion = NotificacionUpdate.newBuilder()
                        .setTipo("NUEVA_OFERTA")
                        .setProducto(request.getProducto())
                        .setNuevoPrecio(request.getMonto())
                        .build();
                notificarATodos(notificacion);
            }
        }
        responseObserver.onNext(RespuestaBooleana.newBuilder().setExito(exito).build());
        responseObserver.onCompleted();
    }

    @Override
    public void obtieneCatalogo(Empty request, StreamObserver<CatalogoRespuesta> responseObserver) {
        CatalogoRespuesta.Builder respuesta = CatalogoRespuesta.newBuilder();
        for (Producto p : productos.values()) {
            ProductoInfo info = ProductoInfo.newBuilder()
                    .setVendedor(p.vendedor)
                    .setProducto(p.nombre)
                    .setPrecioActual(p.precioActual)
                    .build();
            respuesta.addProductos(info);
        }
        responseObserver.onNext(respuesta.build());
        responseObserver.onCompleted();
    }

    @Override
    public void suscribirseNotificaciones(Empty request, StreamObserver<NotificacionUpdate> responseObserver) {
        // Asigna un ID único a este nuevo subscriptor
        String subscriberId = "sub-" + (subscriberIdCounter++);
        System.out.println("Nuevo subscriptor registrado: " + subscriberId);

        // Almacena el observador para poder enviarle mensajes después
        subscriptores.put(subscriberId, responseObserver);

        // NOTA: Para manejar la desconexión, necesitaríamos un try-catch
        // en 'notificarATodos' y eliminar al subscriptor si 'onError' o 'onCompleted'
        // es llamado.
        // gRPC maneja esto de forma más robusta, pero en una implementación simple,
        // el 'responseObserver' quedará "abierto" hasta que el cliente cancele.
    }

    // --- Lógica de Notificación (La solución) ---
    private void notificarATodos(NotificacionUpdate notificacion) {
        System.out.println("Enviando notificación a " + subscriptores.size() + " subscriptores.");
        for (String id : subscriptores.keySet()) {
            StreamObserver<NotificacionUpdate> observer = subscriptores.get(id);
            try {
                observer.onNext(notificacion);
            } catch (Exception e) {
                // Si el cliente se desconectó, su stream estará cerrado.
                System.out.println("Error enviando a " + id + ". Eliminando subscriptor.");
                subscriptores.remove(id);
            }
        }
    }
}