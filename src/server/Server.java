import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public static Map<String, String> clientList = new HashMap<>();
    private ServerSocket serverSocket;
    private Socket socket;

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
                System.out.println("New client has joined!");
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandlers.add(clientHandler);
                poll.execute(clientHandler);
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
