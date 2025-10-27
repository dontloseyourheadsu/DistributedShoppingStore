package grpcapp;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.subasta.*; // Clases generadas
import io.grpc.stub.StreamObserver;

public class ClienteGPRC {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 50051;

        // 1. Crear el canal de comunicación con el servidor
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // Deshabilita SSL/TLS para la prueba
                .build();

        // 2. Crear los stubs
        // Stub bloqueante (síncrono) para llamadas simples
        SubastaServicioGrpc.SubastaServicioBlockingStub blockingStub = SubastaServicioGrpc.newBlockingStub(channel);

        // Stub no bloqueante (asíncrono) para el stream
        SubastaServicioGrpc.SubastaServicioStub asyncStub = SubastaServicioGrpc.newStub(channel);

        // 3. Configurar el MVC
        SubastaVista vista = new SubastaVista();
        SubastaControladorGPRC controlador = new SubastaControladorGPRC(vista, blockingStub);

        vista.asignarActionListener(controlador);
        vista.asignarListSelectionListener(controlador);

        // 4. Iniciar la suscripción a notificaciones (asíncrona)
        suscribirseAActualizaciones(asyncStub, controlador);

        System.out.println("Cliente gRPC conectado y escuchando actualizaciones...");

        // Nota: Deberíamos manejar el channel.shutdown() al cerrar la ventana.
    }

    private static void suscribirseAActualizaciones(
            SubastaServicioGrpc.SubastaServicioStub asyncStub,
            SubastaControladorGPRC controlador) {

        // Observador para manejar las respuestas (notificaciones) del servidor
        StreamObserver<NotificacionUpdate> responseObserver = new StreamObserver<NotificacionUpdate>() {
            @Override
            public void onNext(NotificacionUpdate notificacion) {
                // ¡Solución! El servidor envió una actualización
                System.out.println("NOTIFICACIÓN gRPC RECIBIDA: " + notificacion.getTipo());

                // Forzar al controlador a actualizar la vista
                controlador.actualizarCatalogo();

                // Actualizar el precio si está seleccionado
                controlador.actualizarPrecioSiSeleccionado(
                        notificacion.getProducto(),
                        notificacion.getNuevoPrecio());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error en el stream de notificaciones: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("El servidor cerró el stream de notificaciones.");
            }
        };

        // Llamada al método del servidor (asíncrono)
        asyncStub.suscribirseNotificaciones(Empty.newBuilder().build(), responseObserver);
    }
}
