import java.net.*;
import java.io.*;
import java.util.*;
public class ChatClient  {
    private static String note = " --- ";
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;
    private String server, username;
    private int port;
    public static ArrayList <String> groupUsers;
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    ChatClient(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }
    public boolean start() {
        try {
            socket = new Socket(server, port);
        }
        catch(Exception ec) {
            display("Error connectiong to server:" + ec);
            return false;
        }
        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);
        try
        {
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }
        Listener listen = new Listener();
        Thread l = new Thread(listen);
        l.start();
        try
        {
            sOutput.writeObject(username);
        }
        catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        return true;
    }
    private void display(String msg) {
        System.out.println(msg);
    }
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }
    private void disconnect() {
        try {
            if(sInput != null) {
                sInput.close(); }
        }
        catch(Exception e) {}
        try {
            if(sOutput != null) {
                sOutput.close();
            }
        }
        catch(Exception e) {}
        try{
            if(socket != null) {
                socket.close(); }
        }
        catch(Exception e) {}
    }
    public static void main(String[] args) {
        int portNumber = 1596;
        String serverAddress = "localhost";
        String userName = "Anonymous";
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter the username: ");
        userName = scan.nextLine();
        switch(args.length) {
            case 2:
                portNumber = Integer.parseInt(args[0]);
                serverAddress = args[1];
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                }
                catch(Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                return;
        }
        ChatClient client = new ChatClient(serverAddress, portNumber, userName);
        if(!client.start())
            return;
        System.out.println("\nYou have successfully joined the chatroom!.");
        System.out.println("Type the message to send broadcast to all active clients");
        System.out.println("Type '@username yourmessage' to send a private message");
        System.out.println("Type 'Active' to see list of active clients");
        System.out.println("Type 'Logout' to logoff from server");
        System.out.println("Type 'Group' to be prompted to create a new group - see groups with 'Active'");
        System.out.println("Type 'GroupMessage' to begin sending message to a group");
        while(true) {
            System.out.print("next> ");
            String msg = scan.nextLine();
            if(msg.equalsIgnoreCase("Logout")) {
                client.sendMessage(new ChatMessage(ChatMessage.Logout, ""));
                break;
            }
            else if(msg.equalsIgnoreCase("Active")) {
                client.sendMessage(new ChatMessage(ChatMessage.Active, ""));
            }
            else if (msg.equalsIgnoreCase("Group")) {
                System.out.println("Enter the name of your group");
                String name = scan.nextLine();
                System.out.println("Enter the members of your group separated by a space");
                String members = scan.nextLine();
                client.sendMessage(new ChatMessage(ChatMessage.CreateGroup, name + "$" + members));
            }
            else if (msg.equalsIgnoreCase("GroupMessage")) {
                client.sendMessage(new ChatMessage(ChatMessage.Active, ""));
                System.out.println("\nWhich group do you want to message?\n\n");
                String toGroup = scan.nextLine();
                System.out.println("Your message: ");
                String gmsg = scan.nextLine();
                client.sendMessage(new ChatMessage(ChatMessage.GroupMsg, toGroup + "$" + gmsg));
            }
            else {
                client.sendMessage(new ChatMessage(ChatMessage.Message, msg));
            }
        }
        scan.close();
        client.disconnect();
    }
    class Listener implements Runnable {
        public void run() {
            while(true) {
                try {
                    String msg = sInput.readObject().toString();
                    System.out.println(msg);
                    System.out.print("> ");
                }
                catch(IOException e) {
                    display(note + "Server has closed the connection: " + e + note);
                    break;
                }
                catch(ClassNotFoundException e2) {
                }
            }
        }
    }
}