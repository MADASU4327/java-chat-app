import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Step 1: Handle Name Registration
            while (true) {
                String requestedName = in.readLine();
                if (requestedName == null)
                    return; // Client disconnected

                // Basic validation
                if (requestedName.isEmpty() || ChatServer.isNameTaken(requestedName)) {
                    out.println("NAME_TAKEN"); // Tell client to ask again
                } else {
                    clientName = requestedName;
                    out.println("NAME_ACCEPTED:" + clientName);
                    ChatServer.addClient(clientName, this);
                    break;
                }
            }

            // Step 2: Main Message Loop
            String message;
            while ((message = in.readLine()) != null) {
                // Check if it's a private message (starts with @Name)
                if (message.startsWith("@")) {
                    int spaceIndex = message.indexOf(" ");
                    if (spaceIndex != -1) {
                        String recipientName = message.substring(1, spaceIndex);
                        String content = message.substring(spaceIndex + 1);
                        ChatServer.sendPrivateMessage(clientName, recipientName, content);
                    } else {
                        out.println("MESSAGE:Server:Invalid private message format. Use @Name Message");
                    }
                } else {
                    // Regular public message
                    ChatServer.broadcast("MESSAGE:" + clientName + ":" + message, this);
                }
            }

        } catch (IOException e) {
            System.out.println("Error handling client " + clientName + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ChatServer.removeClient(clientName);
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
