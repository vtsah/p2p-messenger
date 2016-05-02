import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

/**
 * A group chat.
 * Encompases a group of users who are all in a chat together and gives the group a name.
 */
public class Group {
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
    public Group(String name) {
        this.name = name;
        this.users = new ArrayList<User>();
    }

    /**
     * Creates a chat with users given.
     * @param name The name of the group chat.
     * @param input The input stream containing user data.
     * @return Chat after unpacking.
     */
    public static Group unpack(String name, BufferedInputStream input) {
        Group group = new Group(name);
        group.users = new ArrayList<User>();

        try {
            int userCount = IOHelper.getInt(input);
            for (int i = 0; i < userCount; i++) {
                group.users.add(User.unpack(input));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return group;       
    }

    /**
     * Adds a user to the group chat
     * @param user User to be added to group chat.
     */
    public void joinGroup(User user) {
        System.out.println("User "+user.username+" at IP "+user.address+" at port "+user.port+" joined group "+this.name);
        this.users.add(user);
    }

    /**
     * Convert a Chat object into a byte array for sending to a new user.
     * @return A byte[] for transporting over the wire. 
     */
    public byte[] pack() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        
        IOHelper.writeInt(this.users.size(), byteStream);

        System.out.println("There are "+this.users.size()+" users");

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

    /** 
     * Finds the current user, removes her from the group, and returns her.
     * @param userID The ID for the current user.
     * @return User with that ID.
     */
    public User findMe(int userID) {
        int index = 0;
        Iterator<User> userIterator = this.users.iterator();

        while (userIterator.hasNext()) {
            User user = userIterator.next();

            if (user.userID == userID) {
                return this.users.remove(index);
            }

            index++;
        }
        return null;
    }
}
