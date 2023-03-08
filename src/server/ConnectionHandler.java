package server;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectionHandler implements Runnable{

    // this map contains <socket, userNickname>
    private Map<Socket, String> accounts = new HashMap<>();
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String userEmail;
    private String nickname;

    public ConnectionHandler(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // call account() method to take care of user's account
            accountStep();

            // infinite loop to get messages from client
            while(true){
                String msgFromClient = in.readLine();
                if(msgFromClient.equals("/command")){
                    commands();
                }
            }
        } catch (IOException e){
            shutdown();
        } catch (NullPointerException e1){
            // ignore
        }
    }

    /**
     * account creation or login stage
     */
    public void accountStep(){
        try {
            // ask from user whether user has an account or not
            out.println("1. Sign up");
            out.println("2. Sign in");
            String choose = sanitize(in.readLine());
            while (!choose.equals("1") && !choose.equals("2")) {
                out.println("Please select 1 or 2");
                choose = sanitize(in.readLine());
            }

            // ask for user's email address
            out.println("Please enter your email: ");
            String userEmail = sanitize(in.readLine());
            while (!checkEmail(userEmail)) {
                out.println("Please enter a valid email address: ");
                userEmail = sanitize(in.readLine());
            }

            // sign in or log in with that email
            if (choose.equals("1")) {
                signUp(userEmail);
                out.println("A new account has been created successfully!");
                out.println("/stop");
            }
            else {
                signIn(userEmail);
                out.println("Logged in successful!");
                out.println("/stop");
            }

        } catch (IOException e){
            shutdown();
        }
    }

    /**
     * to create an account
     */
    public void signUp(String email){
        try {
            // check if this email is already existed
            while (Server.clientList.containsKey(email)) {
                out.println("This email is already existed in Server");
                out.println("Please try with another one");
                email = sanitize(in.readLine());
            }

            // confirm email by sending 6-digit code
            confirmationEmail(email);

            // ask for nickname from user
            out.println("Please enter a nickname: ");
            nickname = in.readLine();
            while (checkNickname(nickname)) {
                out.println("This nickname is already existed");
                out.println("Please choose another one: ");
                nickname = in.readLine();
            }

            Server.clientHandlers.put(this.nickname, this.socket);

            // save this email and nickname in Server
            Server.clientList.put(email, nickname);

            // save a connection with this user and with user's nickname
            accounts.put(this.socket, nickname);
        } catch (IOException e){
            shutdown();
        }

    }

    /**
     * to enter an existed account
     */
    public void signIn(String email){
        try {
            // check whether this email is available or not
            while (!Server.clientList.containsKey(userEmail)) {
                out.println("This email is not available in Server");
                out.println("Please try with another email");
                userEmail = sanitize(in.readLine());
            }

            // confirm this email by sending 6-digit code
            confirmationEmail(userEmail);
        } catch (IOException e){
            shutdown();
        }
    }

    /**
     * to send 6-digit code for confirmation
     * @param email
     */
    public void confirmationEmail(String email){
        // creating random confirmation code
        String randomCode = createConformationCode();

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", true);
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", 587);
        properties.put("mail.smtp.starttls.enable", true);
        properties.put("mail.transport.protocol", "smtp");

        // code will be sent from this email
        Session session = Session.getInstance(properties, new Authenticator(){
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("otaboyev149@gmail.com", "zoebjqhtjtstfjcb");
            }
        });

        try {

            // preparing message to send
            MimeMessage message = new MimeMessage(session);
            message.setSubject("Confirmation from Messenger applicatoin");
            message.setText("Confirmation code: " + randomCode);

            // adding email address
            Address addressTo = new InternetAddress(email);
            message.setRecipient(Message.RecipientType.TO, addressTo);

            // sending...
            Transport.send(message);

            // ask for confirmation code from user
            out.println("Please enter a confirmation code: ");
            String enteredCode = in.readLine();

            // check if code is valid
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

    /**
     * to crate a 6-digit code
     * @return
     */
    public String createConformationCode(){
        String code = "";
        Random rand = new Random();
        for(int i=0;i<6;i++){
            code = code + rand.nextInt(10);
        }
        return code;
    }

    /**
     * to check whether this email is valid or not
     * @param email
     * @return
     */
    public boolean checkEmail(String email){
        String emailRegex = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
        Pattern emailPattern = Pattern.compile(emailRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = emailPattern.matcher(email);
        return matcher.find();
    }

    /**
     * to check whether this nickname is unique or not
     * @param nickname
     * @return
     */
    public boolean checkNickname(String nickname){
        return Server.clientList.containsValue(nickname);
    }

    /**
     * shutdown() will stop the program
     */
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

    /**
     * commands
     */
    public void commands(){
        try {
            String command = in.readLine();
            if(command.equals("/searchFriend")){
                String nickname = in.readLine();
                searchFromServer(nickname);
            }
            else if(command.equals("/startChat")){
                Socket friend = Server.clientHandlers.get(in.readLine());

                // current client to friend
                Thread thread = new Thread(new SendingMessage(friend));
                thread.start();

                // friend to current client
                startChat(friend);
            }
        } catch (IOException e){
            shutdown();
        }
    }

    /**
     * search for nickname from Server
     * @param nickname
     */
    public void searchFromServer(String nickname){

        // creating ArrayList to save all the matches
        // with the nickname which user entered
        ArrayList<String> resultsList = new ArrayList<>();

        // iterating the Map which contains all the users in Server
        // and printing matches
        for(String nick : Server.clientHandlers.keySet()){
            if(nick.contains(nickname)){
                resultsList.add(nick);
            }
        }

        for (int i=0;i<resultsList.size();i++){
            int index = i+1;
            out.println((index) + ". " + resultsList.get(i));
        }

        // asking to select one option
        out.println("Select right number (0 for none)");
        try {
            String choose = in.readLine();
            int ch = Integer.parseInt(choose);
            while(ch < 0 || ch > resultsList.size()){
                out.println("Please select right number!");
                choose = in.readLine();
                ch = Integer.parseInt(choose);
            }

            if(ch == 0){
                out.println("/command");
                out.println("/backToMenu");
            }
            else {

                out.println("Do you want to chat with " + resultsList.get(ch - 1));
                out.println("1. Yes\n2. No");
                String op = in.readLine();
                int option = Integer.parseInt(op);
                while (option != 1 && option != 2) {
                    out.println("Please select 1 or 2");
                    op = in.readLine();
                    option = Integer.parseInt(op);
                }

                // notifying to stop looping in searching
                out.println("/command");
                out.println("/stopLooping");

                // adding selected friend to user's friends list
                if (option == 1) {
                    out.println("/command");
                    out.println("/addFriend");
                    out.println(resultsList.get(ch - 1));

                    // sending notification to this friend
                    PrintWriter outToFriend = new PrintWriter(Server.clientHandlers.get(resultsList.get(ch - 1)).getOutputStream(), true);
                    outToFriend.println("/command");
                    outToFriend.println("/addedYou");
                    // my nickname
                    outToFriend.println(this.nickname);

                }
                // or canceling
                else {
                    out.println("/command");
                    out.println("/backToMenu");
                }
            }
        } catch (IOException e){

        }
    }

    public void checkOption(String num, int i){
        String s = "";
        for(int i1=0;i1<=i;i1++){
            s += i1;
        }
        while(!s.contains(num)){
            out.println("Please select right number!");
        }
    }

    /**
     * to get a message from friend
     * @param friend
     */
    public void startChat(Socket friend){
        try {
            BufferedReader inFriend = new BufferedReader(new InputStreamReader(friend.getInputStream()));
            String friendNickname = accounts.get(friend);
            String msgToClient = inFriend.readLine();
            while(!sanitize(msgToClient).equals("/quit")){
                out.println(friendNickname + ": " + msgToClient);
                msgToClient = inFriend.readLine();
            }
            out.println("/command");
            out.println("/notOnline");
        } catch (IOException e){
            shutdown();
        }

    }

    /**
     * Class to send a message to a particular friend
     */
    class SendingMessage implements Runnable{
        private Socket friend;
        private PrintWriter outToFriend;
        public SendingMessage(Socket friend){
            this.friend = friend;
        }

        @Override
        public void run() {
            try {
                outToFriend = new PrintWriter(friend.getOutputStream(), true);

                //

                String msgToSend = in.readLine();
                while(!sanitize(msgToSend).equals("/quit")){
                    outToFriend.println(msgToSend);
                    msgToSend = in.readLine();
                }
                outToFriend.println("/command");
                outToFriend.println("/notOnline");
            } catch (IOException e){
                shutdown();
            }
        }
    }

    /**
     * to remove all white spaces from command,
     * and to make them into the same case
     * @param str
     * @return
     */
    public String sanitize(String str){
        return str.replaceAll("\\s", "").toLowerCase();
    }


}
