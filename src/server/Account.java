package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Account {
    private String userEmail;
    private String usernickname;
    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;

    public Account(Socket socket, String userEmail, String usernickname){
        this.socket = socket;
        this.userEmail = userEmail;
        this.usernickname = usernickname;
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
