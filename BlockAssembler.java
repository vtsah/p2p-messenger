import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * A utility class used for assembling many partially-formed blocks.
 */
public class BlockAssembler {
    public AbstractMap<IncompleteMessageTuple, BlockBuilder> blocks;

    public BlockAssembler(boolean multithreaded){
        if(multithreaded){
            this.blocks = new ConcurrentHashMap<IncompleteMessageTuple, BlockBuilder>();
        }else{
            this.blocks = new HashMap<IncompleteMessageTuple, BlockBuilder>();
        }
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
            bb = new BlockBuilder(message.blockSize);
            blocks.put(new IncompleteMessageTuple(message.senderID, message.blockIndex), bb);
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
     * An in-order mapping of sequence numbers to messages.
     * Order is maintained so we can iterate over all the values
     * in O(n) time while remaining sorted.
     */
    public SortedMap<Integer, Message> pieces;

    /**
     * How many messages will be contained once full
     */
    public int blockSize;

    /**
     * Track if this block is a text message or a file (default to text)
     */
    public Message.Type blockType = Message.Type.TEXT;

    public BlockBuilder(int blockSize){
        this.pieces = new TreeMap<Integer, Message>();
        this.blockSize = blockSize;
    }

    /**
     * Add a message to this block builder object.
     * Notice that it will replace messages if a message
     * is added twice (does not check for collisions)
     */
    public void addMessage(Message message){
        pieces.put(message.sequenceNumber, message);
    }

    /**
     * Get this block as a binary representation 
     * (used for files)
     */
    public byte[] getBinary(){

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (Map.Entry<Integer, Message> entry : pieces.entrySet()) {
            Message m = (Message) entry.getValue();
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