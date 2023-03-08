package application;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client implements Runnable{
    private Map<String, File> msgFiles = new HashMap<>();

    // friends list in user's account
    private ArrayList<String> friends = new ArrayList<>();

    // groups list in user's account
    private ArrayList<String> groups = new ArrayList<>();

    private volatile ArrayList<String> notifications = new ArrayList<>();

    // file path to save all messages
    private final String path = "C:\\Users\\otabo\\Documents\\Java\\myProject\\msg_lib";
    private Socket socket;
    private BufferedReader in;
    private Scanner sc;
    private PrintWriter out;
    private ExecutorService poll;
    private boolean done;
    private volatile boolean looping;

    /**
     * Constructor for Client object
     */
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
            sc = new Scanner(System.in);

            // create an account (or)
            // log in to your account
            account();

            // start input messages from Server
            // if message is "/command" call commands function
            // otherwise print message to console
            Thread thread = new Thread(new InputHandler(socket));
            thread.start();

            // start input messages from user
            // call mainWindowCommands() method to get a command
            // from main Window
            mainWindow();


        } catch (IOException e){
            shutdown();
        }


    }

    /**
     * to remove all white spaces from command,
     * and to make them into the same case
     * @param str
     * @return
     */
    public String defaultCase(String str){
        return str.replaceAll("\\s", "").toLowerCase();
    }

    /**
     * to stop working everything
     */
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
            sc.close();
        } catch (IOException e){
            // ignore
        }
    }

    /**
     * class to just print messages
     */
    class OutputToConsole implements Runnable{
        @Override
        public void run() {
            try {
                String msg = in.readLine();
                while (!defaultCase(msg).equals("/stop")){
                    System.out.println(msg);
                    msg = in.readLine();
                }
            } catch (IOException e){
                shutdown();
            }
        }
    }

    /**
     * Message handler given by server
     */
    class InputHandler implements Runnable{
        private Socket socket;

        public InputHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while(socket.isConnected()){
                    String msgFromServer = in.readLine();

                    // if it is "/command" call commands
                    if(msgFromServer.equals("/command")){
                        commands();
                    }
                    // otherwise print to console
                    else {
                        System.out.println(msgFromServer);
                    }
                }
            } catch (IOException e){
                shutdown();
            }
        }
    }

    /**
     * account creation or login stage
     */
    public void account(){
        // creating a thread to print messages from Server
        Thread thread = new Thread(new OutputToConsole());
        thread.start();

        // sending essential information to the server
        while(thread.isAlive()){
            String msg = sc.nextLine();
            out.println(msg);
        }
    }

    /**
     * main window in application
     */
    public void mainWindow(){
        while(!done) {
            clear();
            notifications();
            System.out.println("1. Show main menu");
            System.out.println("2. Write to friend");
            System.out.println("3. Write to group");
            System.out.println("4. Show notifications");

            String msgMainWindow = sc.nextLine();

            if(msgMainWindow.equals("1")){
                clear();
                mainMenu();
            }
            else if(msgMainWindow.equals("2")){
                clear();
                writeToFriend();
            }
            else if(msgMainWindow.equals("3")){
                clear();
                //writeToGroup();
            }
            else if(msgMainWindow.equals("4")){
                clear();
                showNotifications();
            }
            else {
                System.out.println("Please select one of these menu number");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e){
                    // ignore
                }
            }
        }
    }

    /**
     * to show number of new notifications
     */
    public void notifications(){
        if(notifications.size() != 0){
            System.out.println("You have " + notifications.size() + " notifications");
        }
    }

    /**
     * to show all incoming notifications to user
     */
    public void showNotifications(){
        int msgInNotification;
        do {
            if (notifications.size() == 0) {
                System.out.println("You don't have any notification");
                msgInNotification = -1;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e){
                    // ignore
                }
            }
            else {
                int index = 1;
                for(String notification : notifications){
                    System.out.println(index + ". " + notification);
                    index++;
                }
                System.out.println("Select one notification to start direct chat (0 to back)");
                msgInNotification = sc.nextInt();
                while (msgInNotification < 1 || msgInNotification > index){
                    System.out.println("Please select right notification!");
                    msgInNotification = sc.nextInt();
                }
                String str[] = notifications.get(msgInNotification-1).split(" ", 2);
                String nickname = str[0];
                out.println("/command");
                out.println("/startChat");
                out.println(nickname);
                startChat();
            }
        } while(msgInNotification != -1);
    }

    /**
     * to show main Window for user
     */
    public void mainMenu(){
        String msgInMenu = "";
        do {
            clear();
            System.out.println("1. Search for friends");
            System.out.println("2. Search for groups");
            System.out.println("3. Show my friends list");
            System.out.println("4. Show my groups list");
            System.out.println("5. Create a new group");
            System.out.println("6. Settings");
            System.out.println("7. Movements in this app");
            System.out.println("0. Back to the main window");
            try {
                msgInMenu = sc.nextLine();
                if(msgInMenu.equals("1")){
                    clear();
                    searchFriend();
                }
                else if(msgInMenu.equals("2")){

                }
                else if(msgInMenu.equals("3")){

                }
                else if(msgInMenu.equals("4")){

                }
                else if(msgInMenu.equals("5")){

                }
                else if(msgInMenu.equals("6")){

                }
                else if(msgInMenu.equals("7")){

                }
                else if(!msgInMenu.equals("0")){
                    System.out.println("Please select 0 to 7 from the above menu");
                }
            } catch (Exception e){
                shutdown();
            }
        } while(!msgInMenu.equals("0"));
    }

    /**
     * starting chat with friends
     */
    public void writeToFriend(){
        int size = friends.size();

        // if user don't have any friend back to main window
        if(size == 0){
            System.out.println("You don't have any friends yet.");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e){
                // ignore
            }
            clear();
            mainWindow();
        }
        // select one
        else{
            for(int i = 0;i<size;i++){
                System.out.println((i+1) + ". " + friends.get(i));
            }
            int option = sc.nextInt();
            while(option < 1 || option > size){
                System.out.println("Please select from 1 to " + (size-1));
                option = sc.nextInt();
            }

            // start chatting with selected friend
            out.println("/command");
            out.println("/startChat");
            out.println(friends.get(option-1));
            startChat();
        }
    }

    /**
     * search for new friends
     */
    public void searchFriend(){
        System.out.println("Enter a nickname you want to search: ");
        out.println("/command");
        out.println("/searchFriend");

        looping = true;
        while(looping){
            String newFriend = sc.nextLine();
            out.println(newFriend);
        }

    }

    /**
     * all commands came from the Server
     */
    public void commands(){
        try {
            String command = in.readLine();

            // add this nickname to friends list
            if(command.equals("/addFriend")){
                String friend = in.readLine();
                friends.add(friend);

                // create a new file to save all messages
                // with this friend
                createNewFile(friend);
                System.out.println("New friend has been added successfully");
                Thread.sleep(2000);
                mainMenu();
            }

            // call main menu method
            else if(command.equals("/backToMenu")){
                clear();
                mainMenu();
            }

            // if command is "/notOnline",
            // then tell this user about that
            else if(command.equals("/notOnline")){
                System.out.println("This user is not Online!");
            }

            // stop looping in searchFriend() method
            else if(command.equals("/stopLooping")){
                looping = false;
            }

            // notification if someone add me to the friends list
            else if(command.equals("/addedYou")){
                String name = in.readLine();
                friends.add(name);
                createNewFile(name);

                //save notification
                addedYou(name);
            }

            // notification if someone start direct chat
            else if(command.equals("/startedChat")){
                String whoStarted = in.readLine();
                startedChatNotification(whoStarted);
            }
        } catch (IOException e){
            shutdown();
        } catch (InterruptedException e1){
            // ignore
        }
    }

    /**
     * if someone add you to the friends,
     * new notification will be added
     * @param name
     */
    public void addedYou(String name){
        notifications.add(name + " added you to his/her friends list");
    }

    /**
     * if somebody started direct chat,
     * new notification will be added
     * @param str
     */
    public void startedChatNotification(String str){
        notifications.add(str + " started direct chat with you.");
    }

    /**
     * creation of new file to save
     * messages from new friend
     * @param name
     */
    public void createNewFile(String name){
        name = name + ".txt";
        try {
            File newFile = new File(path, name);
            newFile.createNewFile();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * to start chat with friend
     */
    public void startChat(){
        try {
            String message = in.readLine();
            while(!defaultCase(message).equals("/quit")){
                out.println(message);
                message = in.readLine();
            }
            out.println(message);
        } catch (IOException e){
            shutdown();
        }
    }

    /**
     * this method will clean Client screen
     */
    public static void clear(){
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            System.out.print("\033[H\033[2J");
            System.out.flush();
        } catch (IOException e){
            // ignore
        } catch (InterruptedException e1){
            // ignore
        }

    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
