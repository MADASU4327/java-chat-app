# Java Chat Application

A real-time, multi-threaded chat application built using Java, Sockets, and Swing. supporting multiple users, group chat, and private messaging.

## Features

*   **Real-time Communication**: Messages are delivered instantly.
*   **Multi-threading**: Helper threads handle multiple clients simultaneously without blocking correctly.
*   **Group Chat**: Default lobby where everyone talks to everyone.
*   **Private Messaging**: Send secret messages to specific users via `@Name` or double-clicking the sidebar.
*   **Online User List**: Visual sidebar showing who is currently connected.
*   **Unique Identities**: Prevents duplicate usernames.

## Prerequisites

*   Java Development Kit (JDK) 8 or higher.

## How to Run

You need to run the **Server** first, and then as many **Clients** as you want.

### 1. Start the Server
Open a terminal in the project folder:
```bash
javac ChatServer.java ClientHandler.java
java ChatServer
```
You will see: `Server is running on port 12345`

### 2. Start a Client
Open a **new** terminal window:
```bash
javac ChatClient.java
java ChatClient
```
1.  Enter your username in the popup.
2.  Start chatting!

To test with multiple users, open another terminal and run `java ChatClient` again.

## Usage

*   **Public Chat**: Type a message and press Enter. Everyone sees it.
*   **Private Chat**:
    *   **Method A**: Double-click a name in the "Online Users" list.
    *   **Method B**: Type `@Name Message` (e.g., `@Alice Hello secret world`).

## Technical Details

*   **Architecture**: Client-Server
*   **Networking**: Java Sockets (TCP)
*   **GUI**: Java Swing (JFrame, JList, JTextArea)
*   **Concurrency**: `Runnable` interface and `Thread` class for handling client connections.
