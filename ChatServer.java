import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;

    // Map to store connected clients: Name -> Handler
    // We use ConcurrentHashMap because multiple threads (users) will access it at
    // the same time.
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat Server is starting...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("A new user has connected!");
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Register a new user
    public static void addClient(String name, ClientHandler handler) {
        clients.put(name, handler);
        broadcastUserList();
        broadcast("MESSAGE:Server:" + name + " has joined the chat!", null);
    }

    // Remove a user
    public static void removeClient(String name) {
        if (name != null) {
            clients.remove(name);
            broadcastUserList();
            broadcast("MESSAGE:Server:" + name + " has left the chat.", null);
        }
    }

    // Send the list of currently online users to EVERYONE
    // Protocol: "USERS:Alice,Bob,Charlie"
    public static void broadcastUserList() {
        StringBuilder sb = new StringBuilder("USERS:");
        for (String name : clients.keySet()) {
            sb.append(name).append(",");
        }
        // Send to all
        for (ClientHandler client : clients.values()) {
            client.sendMessage(sb.toString());
        }
    }

    // Send a public message to everyone
    // Protocol: "MESSAGE:SenderName:The Message Content"
    public static void broadcast(String message, ClientHandler excludeClient) {
        for (ClientHandler client : clients.values()) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }

    // Send a private message
    // Protocol: "PRIVATE:SenderName:The Message Content"
    public static void sendPrivateMessage(String senderName, String recipientName, String message) {
        ClientHandler recipient = clients.get(recipientName);
        ClientHandler sender = clients.get(senderName);

        if (recipient != null) {
            recipient.sendMessage("PRIVATE:" + senderName + ":" + message);
            // Also show it to the sender so they know it sent
            if (sender != null) {
                sender.sendMessage("PRIVATE_SENT:" + recipientName + ":" + message);
            }
        } else {
            if (sender != null) {
                sender.sendMessage("MESSAGE:Server:User " + recipientName + " not found.");
            }
        }
    }

    // Check if name is taken
    public static boolean isNameTaken(String name) {
        return clients.containsKey(name);
    }
}
