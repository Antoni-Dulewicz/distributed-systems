package com.antek;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class Administartor {
    public void start() throws Exception {
        System.out.println("Administrator panel");

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // queue
        String COPIES_QUEUE = "copies";
        String ADMIN_TEAMS_QUEUE = "admin.teams";
        String ADMIN_SUPPLIERS_QUEUE = "admin.suppliers";

        channel.queueDeclare(COPIES_QUEUE, false, false, false, null);
        channel.queueDeclare(ADMIN_TEAMS_QUEUE, false, false, false, null);
        channel.queueDeclare(ADMIN_SUPPLIERS_QUEUE, false, false, false, null);
        channel.basicQos(1);

        // consumer (handle msg)
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("[COPY] " + message);
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        channel.basicConsume(COPIES_QUEUE, false, consumer);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("message: ");
            String msg = br.readLine();

            if (msg.equalsIgnoreCase("exit")) {
                break;
            }

            System.out.println("Tryb wysyłania: team | supplier | all | exit");
            String line = br.readLine();

            switch (line.toLowerCase()) {
                case "team" -> channel.basicPublish("", ADMIN_TEAMS_QUEUE, null, msg.getBytes());
                case "supplier" -> channel.basicPublish("", ADMIN_SUPPLIERS_QUEUE, null, msg.getBytes());
                case "all" -> {
                    channel.basicPublish("", ADMIN_TEAMS_QUEUE, null, msg.getBytes());
                    channel.basicPublish("", ADMIN_SUPPLIERS_QUEUE, null, msg.getBytes());
                }
                default -> System.out.println("Nieznany tryb");
            }
        }

    }

    public static void main(String[] argv) throws Exception {

        Administartor administartor = new Administartor();
        administartor.start();

    }
}
