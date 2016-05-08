import java.util.*;
/**
 * A utility class for storing when requests were sent out;
 * used for "Braking" the request flow.
 */
public class RequestTracker implements Runnable{

    /**
     * How long do we wait before making a second request?
     */
    static long TIMEOUT = 300;

    public Chat chat;

    /**
     * Map a userID to a mapping of sequence-numbers to send-times.
     */
    public HashMap<Integer, HashMap<Integer, Long>> requests;


    public RequestTracker(Chat chat){
        this.chat = chat;
        requests = new HashMap<Integer, HashMap<Integer, Long>>();
    }

    /**
     * Log that a message (senderID, sequenceNumber) was sent out
     * at a given time.
     */
    public void logRequest(int senderID, int sequenceNumber){
        HashMap<Integer, Long> userBucket = requests.get(senderID);
        if(userBucket == null){
            userBucket = new HashMap<Integer, Long>();
            requests.put(senderID, userBucket);
        }

        userBucket.put(sequenceNumber, System.currentTimeMillis());
    }

    /**
     * Are we allowed to request this message?
     */
    public boolean canRequestMessage(int senderID, int sequenceNumber){
        HashMap<Integer, Long> userBucket = requests.get(senderID);
        if(userBucket == null)
            return true;

        Long timeSent = userBucket.get(sequenceNumber);
        if(timeSent == null)
            return true;

        return System.currentTimeMillis() - timeSent.longValue() > TIMEOUT;
    }

    public void run(){
        while(true){
            try{
                Thread.sleep(TIMEOUT);
            }catch (Exception e){
                System.err.println("RequestTracker: Thread can't sleep");
                e.printStackTrace();
            }
            chat.beInterested();
        }
    }
}