/**
 * Main client.
 * Run with `java Client server-ip server-port chat-name username`
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

/**
 * Group Chat Client.
 */
public class Client {

    /**
     * The Chat this client is connected to.
     */
    public Chat chat;

    /**
     * Creates client for given chat.
     * @param chat Group chat for this client.
     */
    public Client(Chat chat) {
        this.chat = chat;
    }

    /**
     * Starts the P2P Group Chat process.
     */
    public void startMessaging() {
        // TODO: start a thread for receiving data.
        while (true) {
            Scanner userInput = new Scanner(System.in);
            String message = userInput.next();

            System.out.println("Sending "+message);
        }
    }

    /**
     * @param chatName The name of the chat to look for.
     * @param username The user's requested username for the group chat.
     * @return Byte array to send over the wire.s
     */
    public static byte[] serverRequest(String chatName, String username) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            User.writeString(chatName, byteStream);
            User.writeString(username, byteStream);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        
        return byteStream.toByteArray();
    }

    public static void main(String[] args) throws Exception {

        // check arguments
        if (args.length != 4) {
            System.err.println("Usage: java Client server-ip server-port chat-name username");
            return;
        }

        // connect to server to request chat data
        Socket clientSocket;
        InetAddress serverIP = InetAddress.getByName(args[0]);
        int serverPort = Integer.parseInt(args[1]);
        clientSocket = new Socket(serverIP, serverPort);
        BufferedInputStream inFromServer = new BufferedInputStream(clientSocket.getInputStream());
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

        // formulate request.
        byte[] request = serverRequest(args[2], args[3]);
        outToServer.write(request);

        Client client = new Client(Chat.unpack(args[2], inFromServer));

        clientSocket.close();

        // connect to chat (P2P)
        System.out.println("Client connected to chat");

        client.startMessaging();
    }
}



