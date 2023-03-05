import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client implements Runnable{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ExecutorService poll;
    private boolean done;

    public Client(){
        try {
            socket = new Socket("127.0.0.1", 2421);
            poll = Executors.newCachedThreadPool();
            done = false;
        } catch (IOException e){
            shutdown();
        }
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            InputHandler inputHandler = new InputHandler(socket);
            Thread thread = new Thread(inputHandler);
            thread.start();

            String message;
            while(!done){
                message = in.readLine();
                System.out.println(message);
            }
        } catch (IOException e){
            shutdown();
        }


    }
    public void shutdown(){
        done = true;
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
            if (!poll.isShutdown()) {
                poll.shutdown();
            }
            in.close();
            out.close();
        } catch (IOException e){
            // ignore
        }
    }

    class InputHandler implements Runnable{
        private PrintWriter out;
        private Scanner sc;
        Socket socket;

        public InputHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                sc = new Scanner(System.in);
                out = new PrintWriter(socket.getOutputStream(), true);

                String messageToServer;
                while(!done){
                    messageToServer = sc.nextLine();
                    out.println(messageToServer);
                }
            } catch (IOException e){
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
