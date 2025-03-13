package com.antek.model;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;
import java.net.InetAddress;

public class Client {
    private final String id;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private int udpPort;
    private InetAddress udpAddress;

    public Client(String id,Socket socket) throws UnknownHostException {
        this.id = id;
        this.socket = socket;
        this.udpAddress = InetAddress.getByName("::1");
    }

    public String getId() {
        return id;
    }

    public Socket getSocket() {
        return socket;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public InetAddress getUdpAddress() {
        return udpAddress;
    }

    public void setUdpAddress(InetAddress udpAddress) {
        this.udpAddress = udpAddress;
    }

    public void setIn(ObjectInputStream in) {
        this.in = in;
    }

    public void setOut(ObjectOutputStream out) {
        this.out = out;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public ObjectOutputStream getOut() {
        return out;
    }
}
