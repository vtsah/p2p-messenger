import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/**
 * A class to help send/receive files. Mostly text parsing and file I/O.
 */
public class FileSendingUtil {

    /**
     * The chat associated with this file sender
     */
    public Chat chat = null;

    /**
     * The text that the user entered to send a file
     */
    public String message = null;

    /**
     * The file to send
     */
    public File file = null;

    /**
     * Helper function for determining if a line of text signals that
     * the user wants to send a file. If returns true, then an actual
     * instance of this class should be made for handling the request.
     * </br>
     * Protocol for sending files (two ways):
     *     /sendfile /path/to/file.txt
     *     > Done.
     *     
     *     /sendfile
     *     > Enter the path to the file you'd like to send
     *     /path/to/file.txt
     *     > Done.
     * 
     * @param message the text that the user entered
     * @return true if and only if the user wants to send file
     */
    public static boolean userWantsToSendFile(String message){
        String[] tokens = message.split(" ");
        
        if(tokens.length < 1)
            return false;

        return tokens[0].toLowerCase().equals("/sendfile");
    }

    public FileSendingUtil(Chat chat){
        this.chat = chat;
    }

    /**
     * Handle the entire process of sending a file, start to finish
     *
     * @return True if sending the file succeeded, else false.
     */
    public boolean handleSendingFile(String message){
        this.message = message;

        // if format: /sendfile /path/to/file.txt try to load file
        String []tokens = message.split(" ");
        if(tokens.length > 1){
            this.file = new File(tokens[1]);
        }

        InputStreamReader converter = new InputStreamReader(System.in);
        BufferedReader in = new BufferedReader(converter);

        while(file == null || !file.exists()){
            System.out.println("Enter the path to the file you would like to send, or enter 'c' to cancel");
            String line = null;
            try{
                line = in.readLine();
            }catch(Exception e){
                System.err.println("FileSendingUtil: Ran into File IO problems");
                e.printStackTrace();
                return false;
            }

            tokens = line.split(" ");
            if(tokens.length < 1 || tokens[0].equals("c")){
                return false;
            }else{
                file = new File(tokens[0]);
                if(!file.exists()){
                    System.out.println("File does not exist.");
                }
            }
        }

        sendFile();
        return true;
    }

    /**
     * Save this file to the current directory and notify the user.
     *
     * @param data the data to handle
     * @param senderID the ID of the sender
     * @return true if successfully handled, else false
     */
    public boolean handleReceivingFile(byte [] data, int senderID){
        String whereToPutIt = "";
        try{
            // get the name of cwd
            Path currentRelativePath = Paths.get("");
            String cwd = currentRelativePath.toAbsolutePath().toString();

            // get the name of a unique file in the current directory
            whereToPutIt = File.createTempFile(chat.whatsHisName(senderID), ".file", new File(cwd)).getPath();
            
            System.out.println(chat.whatsHisName(senderID)+" sent a file, saving to "+whereToPutIt);

            Files.write(Paths.get(whereToPutIt), data);
            return true;
        }catch(Exception e){
            System.err.println("Couldn't save file "+whereToPutIt);
            e.printStackTrace();
            return false;
        }
    }

    // assume file exists by this point
    private void sendFile(){
        // TODO ASCII progress bar!
        try{
            byte[] fileBinary = Files.readAllBytes(file.toPath());
            this.chat.newBlock(fileBinary);
        }catch(Exception e){
            System.err.println("FileSendingUtil: Couldn't read in file binary");
            e.printStackTrace();
        }
    }
}