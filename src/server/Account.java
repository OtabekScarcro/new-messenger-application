import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Account {
    private String userEmail;
    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;

    public Account(Socket socket, String userEmail){
        this.socket = socket;
        this.userEmail = userEmail;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

        } catch (IOException e){
            shutdown();
        }
    }

    public void shutdown(){
        try {
            if(!socket.isClosed()){
                socket.close();
            }
            in.close();
            out.close();
        } catch (IOException e){
            // ignore
        }
    }

}
