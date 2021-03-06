/**
 * Main client.
 * Run with `java Client server-ip server-port chat-name username`
 */

import java.io.*;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.locks.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;
import java.text.SimpleDateFormat;

/**
 * Group Chat Client.
 * Also contains data on chat client to be shared across threads.
 * Contains data structure of other chat members, the messages received, and the knowledge of which user has which messages.
 * Contains references to relevant threads / runnables, so for example the Receiver of control packets can indirectly call a method on the Sender.
 */
public class Client {

    /**
     * The Chat this client is connected to.
     */
    public Chat chat = null;

    /**
     * The receiver thread for client's control packets
     */
    public Receiver receiver = null;

    /**
     * The thread for sending data
     */
    public Seeder seeder = null;

    /**
     * Chosen username
     */
    public String username = null;

    /**
     * UUID for user, assigned by server to be a user identifier, unique to the group
     */
    public int userUUID = 0;

    public User user = null;

    /**
     * A list of all blocks that we can save right now.
     */
    public Queue<SimpleEntry<byte[], Integer>> pendingBlocksToSave = null;

    /**
     * Creates client.
     */
    public Client(String groupName, InetAddress serverAddress, int serverPort, String username) {
        this.username = username;
        this.receiver = new Receiver(this);
        this.seeder = new Seeder(this);
        Group group = null;
        try {
            group = this.requestGroupInfo(groupName, serverAddress, serverPort);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        this.chat = new Chat(this, group, this.userUUID);

        // notify everyone in the chat that I exist
        this.beLoud();

        // start receiver and seeder threads
        Thread receiverThread = new Thread(this.receiver);
        receiverThread.start();

        Thread seederThread = new Thread(this.seeder);
        seederThread.start();
    }

    /**
     * Make sure everyone knows I exist
     * Give them my IP and ports so they know how to get in touch with me.
     * Do this by sending to their data ports, with TCP, to make sure they get the message.
     * This should be called before starting other threads, so don't need to synchronize.
     */
    public void beLoud() {
        Iterator<Peer> myPeers = this.chat.peers.iterator();
        while (myPeers.hasNext()) {
            Peer peer = myPeers.next();
            peer.giveBusinessCard(this.user);
        }
    }

    /**
     * Sends server request for group info.
     * @param groupName name of group requested to join
     * @param serverAddress IP address of server to request
     * @param serverPort Port on server to request from
     * @return Group to be a part of.
     */
    public Group requestGroupInfo(String groupName, InetAddress serverAddress, int serverPort) throws IOException {
        try {
        // connect to server to request chat data
        Socket clientSocket = new Socket(serverAddress, serverPort);
        BufferedInputStream inFromServer = new BufferedInputStream(clientSocket.getInputStream());
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

        // formulate request.
        byte[] request = serverRequest(groupName);
        outToServer.write(request);

        this.userUUID = IOHelper.getInt(inFromServer);

        // System.out.println("UUID assigned: "+this.userUUID);

        Group group = Group.unpack(groupName, inFromServer);

        clientSocket.close();

        this.user = group.findMe(this.userUUID);

        return group;
        } catch(ConnectException ex) {
            System.out.println("Server is not running...");
            System.exit(0);
        }
        return null;
    }

    /**
     * Starts the P2P Group Chat process.
     * Reads lines from standard input and sends them to everyone.
     */
    public void startMessaging() {
        while (true) {
            Scanner in = new Scanner(System.in);
            try {
                String message = in.nextLine();

                long yourmilliseconds = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");            
                Date resultdate = new Date(yourmilliseconds);

                System.out.println("("+sdf.format(resultdate)+") "+this.user.username+": "+message);

                if(FileSendingUtil.userWantsToSendFile(message)){
                    FileSendingUtil fileSender = new FileSendingUtil(this.chat);
                    fileSender.handleSendingFile(message);
                }else{
                    this.chat.newBlock(message);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
        }
    }

    /**
     * Get the current IP of your machine.
     * Iterates over the machines, IP addresses and finds the public facing IP.
     */

    public InetAddress getCurrentIP() {
        try {
            Socket s = new Socket("173.194.206.139", 80);
            InetAddress currIP = s.getLocalAddress();
            s.close();
            return currIP;
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
/*    public InetAddress getCurrentIP() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                Enumeration<InetAddress> nias = ni.getInetAddresses();
                    while(nias.hasMoreElements()) {
                        InetAddress ia = nias.nextElement();
                        if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress() && ia instanceof InetAddress) {
                            return ia;
                        }
                    }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
*/
    /**
     * @param groupName The name of the chat to look for.
     * @return Byte array to send over the wire.
     */
    public byte[] serverRequest(String groupName) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            IOHelper.writeString(groupName, byteStream);
            IOHelper.writeString(this.username, byteStream);
            IOHelper.writeInt(this.receiver.port, byteStream);
            IOHelper.writeInt(this.seeder.port, byteStream);
            // send the IP address from my point of view.
            // This may be different from receiver IP due to NAT or if Server is running on this machine
            IOHelper.writeByteArray(getCurrentIP().getAddress(), byteStream);

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

        InetAddress serverIP = InetAddress.getByName(args[0]);
        int serverPort = Integer.parseInt(args[1]);

        Client client = new Client(args[2], serverIP, serverPort, args[3]);

        // connect to chat (P2P)
        // System.out.println("Connected to chat "+client.chat.name+" with "+client.chat.peers.size()+" others");
        System.out.println("Connected to chat "+client.chat.name);

        client.startMessaging();
    }
}



