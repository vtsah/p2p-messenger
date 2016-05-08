import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * A utility class used for assembling many partially-formed blocks.
 */
public class BlockAssembler {

    public Chat chat;

    /**
     * The blocks we are building
     */
    public AbstractMap<IncompleteMessageTuple, BlockBuilder> blocks;

    public BlockAssembler(Chat chat, boolean multithreaded){
        this.chat = chat;

        if(multithreaded){
            this.blocks = new ConcurrentHashMap<IncompleteMessageTuple, BlockBuilder>();
        }else{
            this.blocks = new HashMap<IncompleteMessageTuple, BlockBuilder>();
        }
    }

    /**
     * This works just like Chat#beJealous(). Returns a message that is needed, or
     * null if none are needed.
     */
    public Message getNeededMessage(){
        for(Map.Entry<IncompleteMessageTuple, BlockBuilder> entry : blocks.entrySet()){
            BlockBuilder bb = entry.getValue();

            if(bb.isFull())
                continue;
            
            Message m = bb.getLowestUnreceivedMessage(false);
            if(m != null)
                return m;
        }

        return null;
    }

    /**
     * Store this message
     *
     * @param message The message to store
     * @return Is the block pertaining to this message complete?
     */
    public boolean storeMessage(Message message){
        BlockBuilder bb = getBlockBuilder(message.senderID, message.blockIndex);

        // this may be the first message in this block
        if(bb == null){
            bb = new BlockBuilder(this, message.type, message.senderID, message.blockIndex, message.blockOffset, message.blockSize);
            blocks.put(new IncompleteMessageTuple(message.senderID, message.blockIndex), bb);

            if(message.type == Message.Type.FILE && message.senderID != chat.hostID)
                System.out.println(chat.whatsHisName(message.senderID)+" is sending a file...");
        }

        bb.addMessage(message);

        return bb.isFull();
    }

    /**
     * Is the block associated with this message complete?
     *
     * @param senderID the sender of the message
     * @param blockIndex the block to retrieve binary from
     * @return yes if the block is complete
     */
    public boolean isBlockComplete(int senderID, int blockIndex){
        BlockBuilder bb = getBlockBuilder(senderID, blockIndex);

        if(bb == null)
            return false;
        else
            return bb.isFull();
    }

    /**
     * Get the binary associated with this message's block
     * 
     * @param senderID the sender of the message
     * @param blockIndex the block to retrieve binary from
     * @return the binary representation of this message's block
     */
    public byte[] getBinary(int senderID, int blockIndex){
        BlockBuilder bb = getBlockBuilder(senderID, blockIndex);

        if(bb == null)
            return null;

        return bb.getBinary();
    }


    /**
     * Get the text (ASCII) associated with this message's block
     * 
     * @param senderID the sender of the message
     * @param blockIndex the block to retrieve text from
     * @return the text representation of this message's block
     */
    public String getText(int senderID, int blockIndex){
        BlockBuilder bb = getBlockBuilder(senderID, blockIndex);

        if(bb == null)
            return null;

        return bb.getText();
    }

    /**
     * Remove a block from storage.
     *
     * @param senderID The sender of the block to remove
     * @param blockIndex The index of the block to remove
     */
    public void removeBlock(int senderID, int blockIndex){
        blocks.remove(new IncompleteMessageTuple(senderID, blockIndex));
    }

    /**
     * What fraction of this block is formed?
     */
    public double getBlockProgress(int senderID, int blockIndex){
        BlockBuilder bb = getBlockBuilder(senderID, blockIndex);
        if(bb == null)
            return 0.0;

        return bb.getProgress();
    }

    /**
     *
     */
    public boolean blockIsText(int senderID, int blockIndex){
        BlockBuilder inQuestion = getBlockBuilder(senderID, blockIndex);

        if(inQuestion == null)
            return false;

        return inQuestion.isText();
    }

    /**
     *
     */
    public boolean blockIsAFile(int senderID, int blockIndex){
        BlockBuilder inQuestion = getBlockBuilder(senderID, blockIndex);

        if(inQuestion == null)
            return false;

        return inQuestion.isAFile();
    }

    private BlockBuilder getBlockBuilder(int senderID, int blockIndex){
        IncompleteMessageTuple key = new IncompleteMessageTuple(senderID, blockIndex);
        return blocks.get(key);
    }
}

/**
 * Map a user and block index to a set of messages
 * comprising (part of) a block. Entries should only be stored
 * as long as the block is incomplete, then should be deleted
 * to avoid references piling up.
 */
class IncompleteMessageTuple{

    /**
     * Unique Sender identifier
     */
    public int senderID = 0;
    
    /**
     * Which block this is (comprised of many messages).
     */
    public int blockIndex = 0;

    public IncompleteMessageTuple(int senderID, int blockIndex){
        this.senderID = senderID;
        this.blockIndex = blockIndex;
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof IncompleteMessageTuple))
            return false;
        
        IncompleteMessageTuple toCompare = (IncompleteMessageTuple) o;

        return (toCompare.senderID == this.senderID 
            && toCompare.blockIndex == this.blockIndex);
    }

    @Override
    public int hashCode(){
        // assuming senderID <= 1024, no duplicate hashes will be generated
        return (blockIndex << 10) + senderID;
    }
}

/**
 * An object used to efficiently store partially built blocks 
 * as many tiny messages (pieces)
 */
class BlockBuilder {

    /**
     * The "parent" block assembler
     */
    public BlockAssembler blockAssembler;

    /**
     * An in-order mapping of sequence numbers to messages.
     * Order is maintained so we can iterate over all the values
     * in O(n) time while remaining sorted.
     * </br>
     * In the future, we may want to also use a regular hash
     * table, since we frequently do lookups and it's unconfirmed
     * it Java's TreeMap can do O(1) lookup (probably not). For
     * now, O(log(n)) is sufficient (Java uses Red-Black trees)
     */
    public SortedMap<Integer, Message> pieces;

    /**
     * The SenderID of the user who created this block.
     */
    public int senderID;

    /**
     * The index of this block, relative to sender.
     */
    public int blockIndex;

    /**
     * The sequence number of the first message in this block
     */
    public int blockOffset;

    /**
     * The size of this block once full.
     */
    public int blockSize;

    /**
     * Sequence number of the lowest unreceived message.
     * If we track this, we can get the sequence number of
     * an unreceived message in ~O(1) time (assuming) message
     * come relatively in-order.
     */
    public int lowestUnreceivedMessage;

    /**
     * Track if this block is a text message or a file (default to text)
     */
    public Message.Type blockType = Message.Type.TEXT;

    public BlockBuilder(BlockAssembler parent, Message.Type blockType, int senderID, int blockIndex, int blockOffset, int blockSize){
        this.blockAssembler = parent;
        this.blockType = blockType;
        this.pieces = new TreeMap<Integer, Message>();
        this.senderID = senderID;
        this.blockSize = blockSize;
        this.blockOffset = blockOffset;
        this.lowestUnreceivedMessage = blockOffset;
    }

    /**
     * Get the lowest unreceived message if timeout is ignored.
     * If timeout is not ignored, then we find the lowest unreceived
     * message that we aren't still waiting to hear back from.
     */
    public Message getLowestUnreceivedMessage(boolean ignoreTimeout){
        if(ignoreTimeout)
            return new Message(null, null, senderID, blockIndex, blockOffset, blockSize, lowestUnreceivedMessage, 0);

        // find the lowest sequence number that we can request (we didn't just request it) and is unreceived
        int index = lowestUnreceivedMessage;
        while(!this.blockAssembler.chat.requestTracker.canRequestMessage(senderID, index) || pieces.containsKey(index)){
            index++;
        }

        // if this block comprises 5 messages, don't ask for the 6th message
        if(index >= blockOffset + blockSize)
            return null;
        else
            return new Message(null, null, senderID, blockIndex, blockOffset, blockSize, index, 0);        
    }

    /**
     * Add a message to this block builder object.
     * Notice that it will replace messages if a message
     * is added twice (does not check for collisions)
     */
    public void addMessage(Message message){
        pieces.put(message.sequenceNumber, message);

        // find new lowest unreceived message
        while(pieces.containsKey(lowestUnreceivedMessage)){
            lowestUnreceivedMessage++;
        }
    }

    /**
     * Get this block as a binary representation 
     * (used for files)
     */
    public byte[] getBinary(){

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (Map.Entry<Integer, Message> entry : pieces.entrySet()) {
            Message m = entry.getValue();
            if(m.data == null)
                continue;

            os.write(m.data, 0, m.data.length);
        }
        return os.toByteArray();
    }

    /**
     * Get this block as a text representation
     * (used for text messages)
     */
    public String getText(){
        return new String(getBinary(), StandardCharsets.US_ASCII);
    }

    /**
     * What fraction of this block is formed?
     */
    public double getProgress(){
        return ((double) pieces.size())/(blockSize);
    }

    /**
     * Is this blockbuilder full?
     */
    public boolean isFull(){
        return pieces.size() == blockSize;
    }

    public boolean isText(){
        return this.blockType == Message.Type.TEXT;
    }

    public boolean isAFile(){
        return this.blockType == Message.Type.FILE;
    }
}