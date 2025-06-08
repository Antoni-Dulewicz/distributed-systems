package com.smarthome;

import com.smarthome.devices.FridgeServiceImpl;
import com.smarthome.devices.LightServiceImpl;
import com.smarthome.devices.TemperatureServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class GrpcServer {

    private static final Logger logger = Logger.getLogger(GrpcServer.class.getName());

    private String address = "127.0.0.1";
    private int port = 50051;
    private Server server;

    private SocketAddress socket;


    private void start() throws IOException {
        try { socket = new InetSocketAddress(InetAddress.getByName(address), port);	} catch(UnknownHostException e) {};

        server = ServerBuilder.forPort(50051).executor((Executors.newFixedThreadPool(16)))
                //NettyServerBuilder.forAddress(socket).executor(Executors.newFixedThreadPool(16))
                .addService(new TemperatureServiceImpl())
                .addService(new FridgeServiceImpl())
                .addService(new LightServiceImpl())
                .build()
                .start();

        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("Shutting down gRPC server...");
                GrpcServer.this.stop();
                System.err.println("Server shut down.");
            }
        });

    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {

        final GrpcServer server = new GrpcServer();
        server.start();
        server.blockUntilShutdown();

    }
}