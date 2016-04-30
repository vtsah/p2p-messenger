/**
 * A Group contains data needed by the server about a group chat. A Chat contains data needed by a client.
 * Keeps track of all Peers.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class Chat {
    /**
     * Peers currently participating in this group chat
     */
    public ArrayList<Peer> peers = null;

    /**
     * Name of group chat
     */
    public String name = null;

    /**
     * Creates a group chat from the given Group.
     * @param group Data from the Server to initialize a chat among peers.
     */
    public Chat(Group group) {
        this.name = group.name;
        this.peers = new ArrayList<Peer>();


    }
}