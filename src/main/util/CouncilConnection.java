package util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

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
     * @param port : int : the port number to connect to.
     * @return : Socket : the socket connection to the server.
     * @throws IOException : if the connection could not be established.
     */
    public static Socket getConnection(String serverName, int port) throws IOException {
        long startTime = System.currentTimeMillis(); // fetch starting time
        Socket socket = null;
        boolean connected = false;
        // try for 10 seconds before giving up
        while (!connected && System.currentTimeMillis() - startTime < 10000) {
            try {
                socket = new Socket(serverName, port);
                connected = true;
            } catch (ConnectException _) {
                // the server is not up yet, try again
            } catch (IOException e) {
                // rethrow the exception so we can log it and handle it in the caller
                throw new IOException("Could not connect to " + serverName + " on port " + port);
            }
        }
        return socket;
    }


}

