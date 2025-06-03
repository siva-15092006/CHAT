import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class ChatServer {
    private static int uniqueId;
    private static ArrayList<Handler> users; 
    private static HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
    private SimpleDateFormat sdf; 
    private int port; 
    private boolean Continue;
    private String note = " --- "; 
    public ChatServer(int port) {
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        users = new ArrayList<Handler>();
    }
    public void Start() {
        Continue = true;
        try
        {
            ServerSocket serverSocket = new ServerSocket(port);
            while(Continue)
            {
                display("Server waiting for Clients on port " + port + ".");
                Socket socket = serverSocket.accept();
                if(!Continue) {
                    break; }
                Handler h = new Handler(socket);
                users.add(h);
                Thread t = new Thread(h);
                t.start();
            }
            try {
                serverSocket.close();
                for(int i = 0; i < users.size(); ++i) {
                    Handler h = users.get(i);
                    try {
                        h.sInput.close();
                        h.sOutput.close();
                        h.socket.close();
                        }
                    catch(IOException ioE) {}
                }
            }
            catch(Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }
    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }
    private synchronized boolean broadcast(String message) {
        String time = sdf.format(new Date());
        String[] who = message.split(" ",3);
        boolean isPrivate =  false;
        if(who[1].charAt(0)=='@')
            isPrivate = true;
        if(isPrivate==true)
        {
            String receiver=who[1].substring(1, who[1].length());
            message=who[0]+ " /private/ " + who[2];
            String messagef = time + " " + message + "\n";
            boolean found=false;
            for(int y=users.size(); --y>= 0;)
            {
                Handler h1=users.get(y);
                String check=h1.getUsername();
                if(check.equals(receiver))
                {
                    if(!h1.writeMsg(messagef)) {
                        users.remove(y);
                        display("Disconnected Client " + h1.username + " removed from list.");
                    }
                    found=true;
                    break;
                }
            }
            if(found!=true)
            {
                return false;
            }
        }
        else
        {
            String messagef = time + " " + message + "\n";
            System.out.print(messagef);
            for(int i = users.size(); --i >= 0;) {
                Handler h2 = users.get(i);
                if(!h2.writeMsg(messagef)) {
                    users.remove(i);
                    display("Disconnected Client " + h2.username + " removed from list.");
                }
            }
        }
        return true;
    }
    synchronized void remove(int id) {
        String disconnectedClient = "";
        for(int i = 0; i < users.size(); ++i) {
            Handler ct = users.get(i);
            if(ct.id == id) {
                disconnectedClient = ct.getUsername();
                users.remove(i);
                break;
            }
        }
        broadcast(note + disconnectedClient + " has left the chat room." + note);
    }
    public static void main(String[] args) {
        int portNumber = 1596;
        if (args.length > 0) {
            try {
                portNumber = Integer.parseInt(args[0]);
            }
            catch (Exception e) {
                System.out.println("Invalid port number.");
                System.out.println("Usage is java Server [portNumber]");
                return;
            }
        }
        ChatServer server = new ChatServer(portNumber);
        server.Start();
    }
    class Handler implements Runnable {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;
        String date;
        Handler(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streams");
            try
            {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput  = new ObjectInputStream(socket.getInputStream());
                username = sInput.readObject().toString();
                for(int i = 0; i < users.size(); ++i) {
                    Handler ct = users.get(i);
                    if(ct.username == username) {
                        username += "1";
                    }
                }
                broadcast(note + username + " has joined the chat room." + note);
            }
            catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }
            catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }
        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public void run() {
            boolean Continue = true;
            while(Continue) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    break;
                }
                String message = cm.getMessage();
                if (cm.getType() == 1) {
                    broadcast(username + ": " + message);
                }
                if (cm.getType() == 2) {
                    display(username + " disconnected.");
                    Continue = false;
                    break;
                }
                if (cm.getType() == 3) {
                    String[] parse = message.split("\\$",2);
                    String groupName = parse[0];
                    String inGroup = parse[1];
                    ArrayList<String> members = new ArrayList<String>(Arrays.asList(inGroup.split(" ")));
                    map.put(groupName, members);
                    broadcast("Creating a group: " + groupName + " " + "-- Members: " + inGroup);
                }
                if (cm.getType() == 4) {
                    String[] parse = message.split("\\$", 2);
                    String receiver = parse[0];
                    String msg = parse[1];
                    for (Map.Entry<String, ArrayList<String>> entry: map.entrySet()) {
                        String key = entry.getKey();
                        if (receiver.equals(key)) {
                            ArrayList<String> getter = entry.getValue();
                            for (String s: getter) {
                                broadcast(username + "_" + receiver + ": " + "@" +s + " " +  msg);
                            }
                        }
                    }
                }
                if (cm.getType() == 0) {
                    writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                    for(int i = 0; i < users.size(); ++i) {
                        Handler ct = users.get(i);
                        writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
                    }
                    int i = 0;
                    for (Map.Entry<String, ArrayList<String>> entry: map.entrySet()) {
                        String key = entry.getKey();
                        ArrayList<String> getter = entry.getValue();
                        writeMsg("Group: " + (i+1) + ") " + key);
                        i++;
                    }
                }
        }
            remove(id);
            close();
        }
        private void close() {
            try {
                if(sOutput != null) {
                    sOutput.close();
                }
            }
            catch(Exception e) {}
            try {
                if(sInput != null) {
                    sInput.close();
                }
            }
            catch(Exception e) {};
            try {
                if(socket != null) {
                    socket.close();
                }
            }
            catch (Exception e) {}
        }
        private boolean writeMsg(String msg) {
            if(!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
            }
            catch(IOException e) {
                display(note + "Error sending message to " + username + note);
                display(e.toString());
            }
            return true;
        }
    }
}
class ChatMessage implements Serializable{
    static final int Active = 0, Message = 1, Logout = 2, CreateGroup = 3, GroupMsg = 4;
    private int type;
    private String message;
    ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }
    int getType() {
        return type;
    }
    String getMessage() {
        return message;
    }
}