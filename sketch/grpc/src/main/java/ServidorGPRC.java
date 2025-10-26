package grpc.src.main.java;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class ServidorGPRC {

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50051;

        // Construye el servidor gRPC
        Server server = ServerBuilder.forPort(port)
                .addService(new SubastaServicioImpl()) // Añade nuestra lógica
                .build();

        // Inicia el servidor
        server.start();
        System.out.println("Servidor gRPC iniciado en el puerto " + port);

        // Espera a que el servidor termine (bloquea el hilo principal)
        server.awaitTermination();
    }
}