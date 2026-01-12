import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    // UI Components
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField textField;
    private JButton sendButton;
    // New: Sidebar for users
    private JList<String> userList;
    private DefaultListModel<String> userListModel;

    // Networking Components
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String myName;

    private String serverAddress = "127.0.0.1";
    private int port = 12345;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatClient().createAndShowGUI();
        });
    }

    public void createAndShowGUI() {
        frame = new JFrame("Chat Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500); // Made it wider for the sidebar
        frame.setLayout(new BorderLayout());

        // 1. Center: Chat History
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        frame.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        // 2. Right: User List Sidebar
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Add a title to the sidebar
        JPanel sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.add(new JLabel("Online Users:"), BorderLayout.NORTH);
        sidebarPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        sidebarPanel.setPreferredSize(new Dimension(150, 0)); // Fixed width
        frame.add(sidebarPanel, BorderLayout.EAST);

        // Click listener for Private Messaging
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) { // Double click to whisper
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(myName)) {
                        textField.setText("@" + selectedUser + " ");
                        textField.requestFocus();
                    }
                }
            }
        });

        // 3. Bottom: Input
        JPanel bottomPanel = new JPanel(new BorderLayout());
        textField = new JTextField();
        textField.addActionListener(e -> sendMessage());
        bottomPanel.add(textField, BorderLayout.CENTER);

        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
        connectToServer();
    }

    private void connectToServer() {
        // Name registration loop
        while (true) {
            String name = JOptionPane.showInputDialog(
                    frame,
                    "Choose a screen name:",
                    "Screen Name Selection",
                    JOptionPane.PLAIN_MESSAGE);
            if (name == null) {
                // User cancelled the dialog, so we exit the program
                System.exit(0);
            }

            if (name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Name cannot be empty!");
                continue; // Ask again
            }

            try {
                socket = new Socket(serverAddress, port);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Handshake
                out.println(name); // Send name request

                String response = in.readLine();
                if (response != null && response.startsWith("NAME_ACCEPTED:")) {
                    myName = response.substring("NAME_ACCEPTED:".length());
                    frame.setTitle("Chat Application - " + myName);
                    messageArea.append("Connected as " + myName + "\n");
                    break; // Success!
                } else if (response != null && response.equals("NAME_TAKEN")) {
                    JOptionPane.showMessageDialog(frame, "Name is taken! Try another.");
                    socket.close(); // Need to reconnect to try again
                }
            } catch (IOException e) {
                messageArea.append("Could not connect: " + e.getMessage() + "\n");
                return; // Stop trying
            }
        }

        // Start listening
        new Thread(new IncomingMessageListener()).start();
    }

    private void sendMessage() {
        String message = textField.getText();
        if (message != null && !message.trim().isEmpty()) {
            if (out != null) {
                out.println(message);
                // We don't echo our own messages here anymore because the server
                // sends them back to us (for consistency across devices/state)
                // OR if it's private, we handle the local echo separately if needed.
                // For this simple version, we'll let the server confirmation handle it
                // OR we can just append "Me: ..." for public messages if the server didn't echo
                // back (which our server DOESN'T for public).

                // WAIT! Our server logic:
                // Public: Broadcast to others (NOT sender).
                // Private: Send to recipient.

                // So we SHOULD show it locally.
                if (message.startsWith("@")) {
                    // Let the server feedback handle this ("PRIVATE_SENT")
                } else {
                    messageArea.append("Me: " + message + "\n");
                }
                textField.setText("");
            }
        }
    }

    private class IncomingMessageListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("USERS:")) {
                        // Update the sidebar
                        String[] users = message.substring(6).split(",");
                        SwingUtilities.invokeLater(() -> {
                            userListModel.clear();
                            for (String u : users) {
                                if (!u.isEmpty())
                                    userListModel.addElement(u);
                            }
                        });
                    } else if (message.startsWith("MESSAGE:")) {
                        // Normal chat OR System message
                        // Protocol: MESSAGE:SenderName:Content
                        String content = message.substring(8); // Remove "MESSAGE:"
                        // We might want to parse SenderName out, but for now our server sends "Name:
                        // Content" or "Server: Content"
                        // Actually, our Server sends "MESSAGE:Name:Text". Let's format it nicer.
                        int firstColon = content.indexOf(":");
                        if (firstColon != -1) {
                            String sender = content.substring(0, firstColon);
                            String text = content.substring(firstColon + 1);
                            messageArea.append(sender + ": " + text + "\n");
                        } else {
                            messageArea.append(content + "\n");
                        }
                    } else if (message.startsWith("PRIVATE:")) {
                        // Incoming private message
                        // Protocol: PRIVATE:Sender:Content
                        String content = message.substring(8);
                        int firstColon = content.indexOf(":");
                        if (firstColon != -1) {
                            String sender = content.substring(0, firstColon);
                            String text = content.substring(firstColon + 1);
                            messageArea.append("[Private From " + sender + "]: " + text + "\n");
                        }
                    } else if (message.startsWith("PRIVATE_SENT:")) {
                        // Confirmation that we sent a private message
                        // Protocol: PRIVATE_SENT:Recipient:Content
                        String content = message.substring(13);
                        int firstColon = content.indexOf(":");
                        if (firstColon != -1) {
                            String recipient = content.substring(0, firstColon);
                            String text = content.substring(firstColon + 1);
                            messageArea.append("[Private To " + recipient + "]: " + text + "\n");
                        }
                    }
                }
            } catch (IOException e) {
                messageArea.append("Disconnected.\n");
            }
        }
    }
}
