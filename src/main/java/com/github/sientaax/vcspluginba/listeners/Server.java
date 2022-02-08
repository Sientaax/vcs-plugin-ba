package com.github.sientaax.vcspluginba.listeners;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Server extends WebSocketServer{

    private WebSocket connection;
    private ArrayList<AssistantInterface> listener;

    public Server(int port){
        super(new InetSocketAddress(port));
        this.listener = new ArrayList<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake){
        System.out.println(conn + "has opened");
        this.connection = conn;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote){
        System.out.println(conn + " has closed");
    }

    @Override
    public void onMessage(WebSocket conn, String message){
        message = message.trim();
        for(AssistantInterface listener : listener){
            listener.onMessageReceived(message);
        }
        System.out.println("Message " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex){
        System.out.println("Error");
        ex.printStackTrace();
    }

    @Override
    public void onStart(){
        System.out.println("Server started");
    }

    public void shutdown(){
        try {
            stop();
            System.out.println("Server shutdown");
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}
