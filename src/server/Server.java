package server;

import server.ConnectionHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    // this map contains <userNickname, socket>
    public static Map<String, Socket> clientHandlers = new HashMap<>();

    // this map contains <userEmail, userNickname>
    public static Map<String, String> clientList = new HashMap<>();
    private ServerSocket serverSocket;
    private Socket socket;

    // environment variable port number as const

    private ExecutorService poll;

    public Server(){
        try {
            serverSocket = new ServerSocket(2421);
            poll = Executors.newCachedThreadPool();
        } catch (IOException e){
            shutdown();
        }
    }

    @Override
    public void run() {
        while(true) {
            try {
                socket = serverSocket.accept();
                // Connections UID
                System.out.println("New client has joined!");
                // save Logs everytime with connection ID
                ConnectionHandler connectionHandler = new ConnectionHandler(socket);
                poll.execute(connectionHandler);
            } catch (Exception e) {
                shutdown();
            }
        }
    }

    public void shutdown(){
        try {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e){
            // ignore
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        System.out.println("Server is running.....");
        server.run();

    }
}
