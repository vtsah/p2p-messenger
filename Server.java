/**
 * Run with `java Server port`
 *
 * Protocol description, over TCP:
 * Request format:
 * Chat name length n (4 bytes), chat name in ASCII (n bytes), username length m (4 bytes), username in ascii (m bytes)
 * Response format:
 * Number of users (4 bytes), List of users: {IP address length n (4 bytes), IP address (n bytes), Port (4 bytes), username length m (4 bytes), username in ascii (m bytes)}
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

/**
 * Chat Server.
 * Keeps track of all ongoing chats and people in them.
 */
public class Server {

    /**
     * Ongoing chats, searchable by chat name.
     */
    private HashMap<String, Chat> groupChats = null;

    /**
     * Creates server and runs it.
     * @param port The local port for this server to listen on
     */
    public Server(int port) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(port, 1);
        System.out.println("Server started; listening at port " + port);

        this.groupChats = new HashMap<String, Chat>();

        while (true) {
            // accept connection from connection queue
            Socket connectionSocket = welcomeSocket.accept();
            System.out.println("accepted connection from " + connectionSocket);

            // create read stream to get input
            BufferedInputStream inFromClient = new BufferedInputStream(connectionSocket.getInputStream());
            // create write stream to send output
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

            String chatName = User.getString(inFromClient);
            String username = User.getString(inFromClient);

            InetAddress ip = connectionSocket.getInetAddress();
            int userPort = connectionSocket.getPort();

            User newUser = new User(username, ip, userPort);

            Chat currentChat = this.groupChats.get(chatName);
            if (currentChat == null) {
                currentChat = new Chat(chatName);
                this.groupChats.put(chatName, currentChat);
            }

            outToClient.write(currentChat.pack());

            currentChat.joinChat(newUser);

            connectionSocket.close();
        }
    }

    public static void main(String[] args) throws Exception {

        // check arguments
        if (args.length != 1) {
            System.err.println("Usage: java Server port");
            return;
        }

        int port = Integer.parseInt(args[0]);

        Server myRunningServer = new Server(port);
    }
}
