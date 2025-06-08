package com.antek;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.List;

public class Supplier {
    private String supplierName;
    private List<EquipmentType> availableEquipment;
    private int internalOrderId = 0;

    public Supplier(String supplierName, List<EquipmentType> availableEquipment) {
        this.supplierName = supplierName;
        this.availableEquipment = availableEquipment;
    }

    public void start() throws Exception {
        System.out.println(supplierName);

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();

        Channel orderChannel = connection.createChannel();

        // queue
        String ORDERS_QUEUE = "orders";
        orderChannel.queueDeclare(ORDERS_QUEUE, false, false, false, null);
        orderChannel.basicQos(1);

        // consumer (handle msg)
        Consumer orderConsumer = new DefaultConsumer(orderChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String[] parts = new String(body, "UTF-8").split(":");

                String teamName = parts[0];
                EquipmentType equipmentType = EquipmentType.valueOf(parts[1]);

                if(!availableEquipment.contains(equipmentType)){
                    System.out.println("Brak sprzętu: " + equipmentType);
                    orderChannel.basicNack(envelope.getDeliveryTag(), false, true);
                    return;
                }

                internalOrderId += 1;
                System.out.println(supplierName + " obsługuje zamówienie od " + teamName + " na " + equipmentType);
                String confirmQueue = "confirm." + teamName;
                String confirmation = supplierName + " zrealizował zlecenie #" + internalOrderId + " dla " + teamName;
                orderChannel.queueDeclare(confirmQueue, false, false, false, null);
                orderChannel.basicPublish("", confirmQueue, null, confirmation.getBytes());

                String copiesQueue = "copies";
                orderChannel.queueDeclare(copiesQueue, false, false, false, null);
                orderChannel.basicPublish("", copiesQueue, null, confirmation.getBytes());


                orderChannel.basicAck(envelope.getDeliveryTag(), false);

            }
        };

        // start listening
        System.out.println("Waiting for messages...");
        orderChannel.basicConsume(ORDERS_QUEUE, false, orderConsumer);

        Channel adminChannel = connection.createChannel();

        // queue
        String adminQueue = "admin.suppliers";
        adminChannel.queueDeclare(adminQueue, false, false, false, null);
        adminChannel.basicQos(1);

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

        List<EquipmentType> equipmentTypeList = switch (argv[1]) {
            case "0" -> List.of(EquipmentType.OXYGEN,EquipmentType.SHOES);
            case "1" -> List.of(EquipmentType.OXYGEN,EquipmentType.BACKPACK);
            case "2" -> List.of(EquipmentType.SHOES,EquipmentType.BACKPACK);
            default -> null;
        };

        Supplier supplier = new Supplier(argv[0], equipmentTypeList);
        supplier.start();

    }

}
