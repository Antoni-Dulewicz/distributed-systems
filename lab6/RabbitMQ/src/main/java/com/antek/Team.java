package com.antek;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Team {
    public static void sendOrder(String teamName, EquipmentType equipmentType) throws Exception {
        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try(Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {

            String ORDERS_QUEUE = "orders";
            String COPIES_QUEUE = "copies";
            channel.queueDeclare(ORDERS_QUEUE, false, false, false, null);
            channel.queueDeclare(COPIES_QUEUE, false, false, false, null);

            Order order = new Order(teamName, equipmentType);
            String orderMessage = order.toMessage();
            channel.basicPublish("", ORDERS_QUEUE, null, orderMessage.getBytes());
            channel.basicPublish("", COPIES_QUEUE, null, orderMessage.getBytes());
            System.out.println("Sent: " + orderMessage);

        }
    }

    public static void listenForConfirmation(String teamName) throws Exception{
        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();


        Channel confirmChannel = connection.createChannel();

        // queue
        String confirmQueue = "confirm." + teamName;
        confirmChannel.queueDeclare(confirmQueue, false, false, false, null);
        confirmChannel.basicQos(1);

        // consumer (handle msg)
        Consumer confirmConsumer = new DefaultConsumer(confirmChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("[CONFIRMATION]: " + message);
                confirmChannel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        confirmChannel.basicConsume(confirmQueue, false, confirmConsumer);

        Channel adminChannel = connection.createChannel();

        String adminQueue = "admin.teams";
        adminChannel.queueDeclare(adminQueue, false, false, false, null);
        adminChannel.basicQos(1);

        // consumer (handle msg)
        Consumer adminConsumer = new DefaultConsumer(adminChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("[ADMIN]: " + message);
                adminChannel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        adminChannel.basicConsume(adminQueue, false, adminConsumer);
    }

    public static void main(String[] argv) throws Exception {
        String teamName = argv[0];
        System.out.println(teamName);

        // start confirmation listener
        new Thread(() -> {
            try {
                listenForConfirmation(teamName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // input loop
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("shoes | oxygen | backpack | exit ");

        while (true) {
            String message = br.readLine();

            if (message.equalsIgnoreCase("exit")) {
                break;
            }

            EquipmentType type = switch (message.toLowerCase()) {
                case "shoes" -> EquipmentType.SHOES;
                case "oxygen" -> EquipmentType.OXYGEN;
                case "backpack" -> EquipmentType.BACKPACK;
                default -> null;
            };

            if (type != null) {
                sendOrder(teamName, type);
            } else {
                System.out.println("Nieznany sprzęt: " + message);
            }
        }
    }
}
