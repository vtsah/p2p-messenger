import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

/**
 * A group chat.
 */
public class Chat {
    /**
     * Users currently participating in this group chat
     */
    public ArrayList<User> users = null;

    /**
     * Name of group chat
     */
    public String name = null;

    /**
     * Creates an empty group chat.
     * @param name The name of the group chat.
     */
    public Chat(String name) {
        this.name = name;
        this.users = new ArrayList<User>();
    }

    /**
     * Creates a chat with users given.
     * @param name The name of the group chat.
     * @param input The input stream containing user data.
     * @return Chat after unpacking.
     */
    public static Chat unpack(String name, BufferedInputStream input) {
        Chat chat = new Chat(name);
        chat.users = new ArrayList<User>();

        try {
            int userCount = User.getInt(input);
            for (int i = 0; i < userCount; i++) {
                // IP address is n bytes
                int ipLength = User.getInt(input);
                byte[] ip = User.getBytes(input, ipLength);
                InetAddress ipAddress = InetAddress.getByAddress(ip);

                int port = User.getInt(input);

                String username = User.getString(input);

                User newUser = new User(username, ipAddress, port);

                chat.users.add(newUser);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return chat;       
    }

    /**
     * Adds a user to the group chat
     * @param user User to be added to group chat.
     */
    public void joinChat(User user) {
        System.out.println("User "+user.username+" at IP "+user.address+" at port "+user.port+" joined chat "+this.name);
        this.users.add(user);
    }

    /**
     * Convert a Chat object into a byte array for sending to a new user.
     * @return A byte[] for transporting over the wire. 
     */
    public byte[] pack() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        
        User.writeInt(this.users.size(), byteStream);

        Iterator<User> userIterator = this.users.iterator();
        while (userIterator.hasNext()) {
            User user = userIterator.next();
            try {
                byteStream.write(user.pack());
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
            
        }

        return byteStream.toByteArray();
    }
}