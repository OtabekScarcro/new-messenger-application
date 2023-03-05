import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable{

    private ArrayList<Account> accounts = new ArrayList<>();
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;
    private String userEmail;
    private String nickname;

    public ClientHandler(Socket socket){
        this.socket = socket;
        done = false;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("1. Sign in");
            out.println("2. Log in");

            String choose = in.readLine();
            while (!choose.equals("1") && !choose.equals("2")) {
                out.println("Please select 1 or 2");
                choose = in.readLine();
            }

            out.println("Please enter your email: ");
            String userEmail = in.readLine();
            while (!checkEmail(userEmail)) {
                out.println("Please enter a valid email address: ");
                userEmail = in.readLine();
            }

            if (choose.equals("1")) {
                while(Server.clientList.containsKey(userEmail)){
                    out.println("This email is already existed in Server");
                    out.println("Please try with another one");
                    userEmail = in.readLine();
                }
                confirmationEmail(userEmail);
                out.println("Please enter a nickname: ");
                nickname = in.readLine();
                while (checkNickname(nickname)) {
                    out.println("This nickname is already existed");
                    out.println("Please choose another one: ");
                    nickname = in.readLine();
                }
                Server.clientList.put(userEmail, nickname);

                Account newAccount = new Account(socket, userEmail);
                accounts.add(newAccount);

                out.println("A new account has been created successfully!");
            } else {
                while(!Server.clientList.containsKey(userEmail)) {
                    out.println("This email is not available in Server");
                    out.println("Please try with another email");
                    userEmail = in.readLine();
                }
                confirmationEmail(userEmail);
                out.println("Logged in successful!");
            }
        } catch (IOException e){
            shutdown();
        }
    }

    public void confirmationEmail(String email){
        String randomCode = conformationCode();

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", true);
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", 587);
        properties.put("mail.smtp.starttls.enable", true);
        properties.put("mail.transport.protocol", "smtp");

        Session session = Session.getInstance(properties, new Authenticator(){
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("otaboyev149@gmail.com", "zoebjqhtjtstfjcb");
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setSubject("Confirmation from Messenger applicatoin");
            message.setText("Confirmation code: " + randomCode);

            Address addressTo = new InternetAddress(email);
            message.setRecipient(Message.RecipientType.TO, addressTo);

            Transport.send(message);

            out.println("Please enter a confirmation code: ");
            String enteredCode = in.readLine();
            while(!enteredCode.equals(randomCode)){
                out.println("Confirmation code is not valid!");
                out.println("Please try again: ");
                enteredCode = in.readLine();
            }
            out.println("Email confirmation is successful");

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String conformationCode(){
        String code = "";
        Random rand = new Random();
        for(int i=0;i<6;i++){
            code = code + rand.nextInt(10);
        }
        return code;
    }

    public boolean checkEmail(String email){
        String emailRegex = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
        Pattern emailPattern = Pattern.compile(emailRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = emailPattern.matcher(email);
        return matcher.find();
    }

    public boolean checkNickname(String nickname){
        return Server.clientList.containsValue(nickname);
    }
    public void shutdown(){
        done = true;
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
