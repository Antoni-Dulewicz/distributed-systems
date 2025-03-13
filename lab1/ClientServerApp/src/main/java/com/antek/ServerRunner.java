package com.antek;
import com.antek.model.Client;
import com.antek.model.Data;
import com.antek.model.Requests;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ServerRunner {

    private static final int PORT = 8080;
    private static List<Client> clients = new ArrayList<>();

    public synchronized static void addClient(Client client) {
        clients.add(client);
    }

    public synchronized static void removeClient(Client client) {
        if(client == null) return;
        try {
            client.getSocket().close();
            clients.remove(client);
            System.out.println("[SERVER]: client <" + client.getId() + "> disconnected.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static List<Client> getClients() {
        return new ArrayList<>(clients);
    }

    public synchronized static Client getClientBySocket(Socket socket){
        for(Client client : getClients()){
            if(client.getSocket().equals(socket)){
                return client;
            }
        }
        return null;
    }

    public synchronized static Client getClientByUdpSenderPort(int udpPort){
        for(Client client : getClients()){
            if(client.getSocket().getPort() == udpPort){
                return client;
            }
        }
        return null;
    }

    public static void run() throws IOException {
        System.out.println("[SERVER]: server started");

        ServerSocket serverSocket = null;
        DatagramSocket udpSocket;

        try {

            // create socket
            serverSocket = new ServerSocket(PORT);
            udpSocket = new DatagramSocket(PORT);

            new Thread(() -> handleUdp(udpSocket)).start();

            while(true){

                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleTcp(clientSocket)).start();

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if ( serverSocket != null){
                serverSocket.close();
            }
        }

    }

    private static void handleTcp(Socket socket){
        try{

            //in & out streams
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            while(true){
                Data input = (Data) in.readObject();

                switch (input.requestType()){
                    case ID:
                        String newId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                        System.out.println("[SERVER TCP]: client<"+ newId +"> connected from " + socket.getInetAddress() + ":" + socket.getPort());
                        registerClient(socket, input, newId, out, in);
                        break;

                    case BROADCAST:
                        System.out.println("[SERVER TCP]: broadcasting...");
                        broadcastUsingTcp(socket, input);
                        break;


                    case DISCONNECT:
                        System.out.println("[SERVER TCP]: disconnecting...");
                        removeClient(Objects.requireNonNull(getClientBySocket(socket)));
                        return;

                    default:
                        throw new IllegalStateException("Unexpected value: " + input.requestType());
                }
            }

        } catch (Exception e) {
            System.out.println("[SERVER]: Connection lost with " + socket.getInetAddress() + ":" + socket.getPort());
            removeClient(Objects.requireNonNull(getClientBySocket(socket)));
        } finally {
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void registerClient(Socket socket, Data input, String newId, ObjectOutputStream out, ObjectInputStream in) throws IOException {
        Data response;
        int udpPort = Integer.parseInt(input.message());

        Client newClient = new Client(newId, socket);
        newClient.setOut(out);
        newClient.setIn(in);
        newClient.setUdpPort(udpPort);
        addClient(newClient);

        response = new Data(Requests.ID, newId);
        out.writeObject(response);
        out.flush();
    }

    private static void broadcastUsingTcp(Socket socket, Data input) throws IOException {
        String senderID = Objects.requireNonNull(getClientBySocket(socket)).getId();
        for(Client client : getClients()){
            if(!client.getSocket().equals(socket)){
                ObjectOutputStream clientOut = client.getOut();
                String outputMessage = "\r["+senderID+"]: " + input.message();
                Data message = new Data(Requests.UNICAST, outputMessage);
                clientOut.writeObject(message);
                clientOut.flush();
            }
        }
    }

    private static void handleUdp(DatagramSocket udpSocket){
        try{
            while(true){
                byte[] receiveBuffer = new byte[20000];

                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(receivePacket);

                System.out.println("[SERVER UDP]: Received packet from " +
                        receivePacket.getAddress() + ":" + receivePacket.getPort());

                broadcastUsingUdp(udpSocket, receivePacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void broadcastUsingUdp(DatagramSocket udpSocket, DatagramPacket receivePacket) throws IOException {
        List<Client> clients = getClients();
        String senderId = Objects.requireNonNull(getClientByUdpSenderPort(receivePacket.getPort())).getId();

        String outputMessage = "\r["+senderId+" (UDP)]: " + new String(receivePacket.getData(), 0, receivePacket.getLength());

        byte[] sendBuffer = outputMessage.getBytes();

        System.out.println("[SERVER UDP]: broadcasting...");
        for(Client client : clients){
            if(receivePacket.getPort() + 1 != client.getUdpPort()){
                System.out.println("[SERVER UDP]: Sending packet to " +client.getUdpAddress()+ ":" + client.getUdpPort());
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length ,client.getUdpAddress(),client.getUdpPort());
                udpSocket.send(sendPacket);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        run();
    }
}
