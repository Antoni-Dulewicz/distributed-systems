package com.antek;

import com.antek.model.Data;
import com.antek.model.Requests;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientRunner{
    private volatile boolean running = true;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private DatagramSocket udpSend;
    private DatagramSocket udpListen;

    public void run() {
        //server address and port
        String SERVERIP = "127.0.0.1";
        int SERVERPORT = 8080;

        //starting client
        System.out.println("[CLIENT]: client started");
        System.out.println("[CLIENT]: creating id request to server...");

        try{

            //create TCP socket
            socket = new Socket(SERVERIP, SERVERPORT);

            System.out.println("[CLIENT TCP]: connected to server at " + SERVERIP + ":" + SERVERPORT);
            System.out.println("[CLIENT TCP]: local socket info: " + socket.getLocalAddress() + ":" + socket.getLocalPort());

            //create UDP sockets for sending and listening - sending is on the same port as TCP
            udpSend = new DatagramSocket(socket.getLocalPort());
            udpListen = new DatagramSocket(socket.getLocalPort() + 1);

            System.out.println("[CLIENT UDP]: listening on " +
                    udpListen.getLocalAddress() + ":" + udpListen.getLocalPort());

            // in & out streams for TCP
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // send udpSocket info
            //there is no need for sending udpPort, but we have to send ID request to register client on the server
            String udpPort =  String.valueOf(udpListen.getLocalPort());
            Data initData = new Data(Requests.ID, udpPort);
            out.writeObject(initData);
            out.flush();

            //read response
            Data response = (Data) in.readObject();
            System.out.println("[SERVER]: client <"+response.message()+"> connected, type 'enter' to send, type 'exit' to disconnect, type 'u' to use UDP");

            //listening for messages sent by server by TCP
            new Thread(this::listenForMessagesTcp).start();
            //listening for messages sent by server by UDP
            new Thread(this::listenForMessagesUdp).start();


            Scanner scanner = new Scanner(System.in);

            //console for read write operations
            while(true){
                System.out.print("[you]: ");
                String userInput = scanner.nextLine();

                if("u".equalsIgnoreCase(userInput)){
                    // sending using UDP
                    while(true){
                        System.out.print("[you (UDP)]: ");
                        //using multiline to be able to send multiline data - ASCII art
                        StringBuilder multiLineInput = new StringBuilder();
                        String line;

                        while(true){
                            line = scanner.nextLine();
                            if("end".equalsIgnoreCase(line)){
                                break;
                            }

                            multiLineInput.append(line).append("\n");
                        }
                        //if multiline input is empty we don't send data
                        if(!multiLineInput.isEmpty()){
                            byte[] sendBuffer = multiLineInput.toString().getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, InetAddress.getByName(SERVERIP), SERVERPORT);
                            udpSend.send(sendPacket);
                            break;
                        }

                    }

                } else if ("exit".equalsIgnoreCase(userInput)) {
                    running = false;

                    System.out.println("[CLIENT]: disconnecting...");
                    Data request = new Data(Requests.DISCONNECT,userInput);
                    out.writeObject(request);
                    out.flush();

                    closeConnections();

                    break;

                }else{
                    // send broadcast to server (TCP)
                    Data request = new Data(Requests.BROADCAST,userInput);
                    out.writeObject(request);
                    out.flush();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }

    }


    private void listenForMessagesTcp(){
        try{
            while(running){
                //check if socket closed
                if (socket.isClosed()) break;
                Data message = (Data) in.readObject();
                System.out.println(message.message());
                System.out.print("[you]: " );

            }
        } catch (Exception e) {
            System.out.println("[CLIENT]: client disconnected");
        }
    }

    private void listenForMessagesUdp(){
        try{

            while(running){
                byte[] receiveBuffer = new byte[20000];

                DatagramPacket receivePacket =
                        new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpListen.receive(receivePacket);

                System.out.println();

                String msg = new String(receivePacket.getData(),0,receivePacket.getLength());
                System.out.println(msg);
                System.out.print("[you]: " );
            }
        } catch (Exception e) {
            System.out.print("");
        }
    }

    private void closeConnections() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (udpListen != null && !udpListen.isClosed()) {
                udpListen.close();
            }
            if (udpSend != null && !udpSend.isClosed()) {
                udpSend.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
