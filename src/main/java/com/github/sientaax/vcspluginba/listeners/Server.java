package com.github.sientaax.vcspluginba.listeners;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Server extends WebSocketServer{

    private WebSocket connection;
    private ArrayList<AssistantInterface> listeners;

    public Server(int port) {
        super(new InetSocketAddress(port));
        this.listeners = new ArrayList<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake){
        this.connection = conn;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote){
        System.out.println(conn + " has closed");
    }

    @Override
    public void onMessage(WebSocket conn, String message){
        for(AssistantInterface listener : listeners){
            listener.onMessageReceived(message);
        }
        Main.receivedMessage(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex){
        ex.printStackTrace();
    }

    @Override
    public void onStart(){
        System.out.println("Server started");
    }

    public void sendMessage(String message){
        if(connection != null && connection.isOpen()){
            connection.send(message);
            for(AssistantInterface listener : listeners){
                listener.onMessageSent(message);
            }
        }
    }

    public void shutdown(){
        try {
            stop();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}