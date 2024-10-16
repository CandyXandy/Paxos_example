package util;

import member.Members;
import message.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * This class is a utility class that provides static methods for facilitating connections between
 * members of the Adelaide Suburbs Council.
 */
public class CouncilConnection {

    /**
     * This class is a utility class and should not be instantiated.
     */
    CouncilConnection() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * This method returns a connection to the server with the given server name and port.
     * It will try to connect for 10 seconds before giving up.
     *
     * @param serverName : String : the name of the server to connect to.
     * @param port       : int : the port number to connect to.
     * @return : Socket : the socket connection to the server.
     * @throws IOException : if the connection could not be established.
     */
    public static Socket getConnection(String serverName, int port) throws IOException {
        long startTime = System.currentTimeMillis(); // fetch starting time
        Socket socket = null;
        boolean connected = false;
        // try for 5 seconds
        while (!connected && System.currentTimeMillis() - startTime < 10000) {
            try {
                socket = new Socket(serverName, port);
                connected = true;
            } catch (ConnectException _) {
                // the server is not up yet, try again
            } catch (IOException e) {
                // rethrow the exception so we can handle it in the caller, log it here
                Logger.getLogger(CouncilConnection.class.getName()).fine("Could not connect to " +
                        serverName + " on port " + port);
                throw new IOException("Could not connect to " + serverName + " on port " + port);
            }
        }
        return socket;
    }


    /**
     * This method reads a message from the BufferedReader and returns a Message object.
     * The message is expected to be in the format "MESSAGE <member number>:<proposal number>".
     * We read the message in a separate thread, so we can close the socket if it takes too long, or
     * we can potentially get blocked forever. We time out after 10 seconds.
     *
     * @param clientSocket : Socket : the socket to read the message from.
     * @return : Message : the message object created from the message.
     * @throws IOException : if the message could not be read from the BufferedReader.
     */
    public static Message readMessage(Socket clientSocket) throws IOException, InterruptedException {
        AtomicReference<String> message = new AtomicReference<>();
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        // read the message in a separate thread, so we can time out if it takes too long
        long startTime = System.currentTimeMillis(); // fetch starting time
        new Thread(() -> {
            try {
                message.set(in.readLine());
            } catch (IOException _) {
                // die quietly
            }
        }).start();
        // wait for the message to be read or time out after 10 seconds
        while (message.get() == null && (System.currentTimeMillis() - startTime) < 10000) {
            Thread.onSpinWait();
            Thread.sleep(1000); // only check every second to avoid murdering the CPU
        }
        if (message.get() == null) {
            // if the message is still null, we timed out
            clientSocket.close(); // close the connection
            throw new IOException("Timed out while trying to read a message.");
        }
        String[] parts = message.get().split(" ");
        String messageType = parts[0];
        String[] proposalParts = parts[1].split(":");
        Members sender = Members.getMemberFromPort(Integer.parseInt(proposalParts[0]));
        int proposalNum = Integer.parseInt(proposalParts[1]);
        String messageValue = parts[2];
        if (messageValue.equals("_")) { // if the message value is "_", it means the message has no value
            return new Message(proposalNum, sender, messageType, null);
        } // otherwise, the message has a value, extract it as a Members constant
        Members value = Members.getMember(Integer.parseInt(messageValue));
        return new Message(proposalNum, sender, messageType, value);
    }

}
