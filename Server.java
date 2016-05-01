/**
 * Run with `java Server port`
 *
 * Protocol description, over TCP:
 * Request format:
 * Group name length n (4 bytes), group name in ASCII (n bytes), username length m (4 bytes), username in ascii (m bytes), control port (4 bytes), data port (4 bytes)
 * Response format:
 * Unique user ID (4 bytes), Number of users (4 bytes), List of users: {IP address length n (4 bytes), IP address (n bytes), Port (4 bytes), username length m (4 bytes), username in ascii (m bytes)}
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
    private HashMap<String, Group> groups = null;

    /**
     * Creates server and runs it.
     * @param port The local port for this server to listen on
     */
    public Server(int port) throws Exception {

        int nextUUID = 0;

        // set up welcome socket and run server
        ServerSocket welcomeSocket = new ServerSocket(port, 4);
        System.out.println("Server started; listening at port " + port);

        this.groups = new HashMap<String, Group>();

        while (true) {
            // accept connection from connection queue
            Socket connectionSocket = welcomeSocket.accept();
            System.out.println("accepted connection from " + connectionSocket);

            // create read stream to get input
            BufferedInputStream inFromClient = new BufferedInputStream(connectionSocket.getInputStream());
            // create write stream to send output
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

            String groupName = IOHelper.getString(inFromClient);
            String username = IOHelper.getString(inFromClient);

            InetAddress ip = connectionSocket.getInetAddress();
            int userPort = IOHelper.getInt(inFromClient);
            int dataPort = IOHelper.getInt(inFromClient);

            User newUser = new User(username, ip, userPort, dataPort, nextUUID);

            Group currentGroup = this.groups.get(groupName);
            if (currentGroup == null) {
                currentGroup = new Group(groupName);
                this.groups.put(groupName, currentGroup);
            }

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

            IOHelper.writeInt(nextUUID, byteStream);

            outToClient.write(byteStream.toByteArray());

            outToClient.write(currentGroup.pack());

            currentGroup.joinGroup(newUser);

            connectionSocket.close();

            nextUUID++;
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
