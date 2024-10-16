package util;

import message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

public class CouncilConnectionTest {

    private final static String HOST_DEFAULT = "localhost";
    private final static int PORT_DEFAULT = 8080;


    /**
     * Tests that a connection can be established to a socket created by the
     * GetConnection method.
     */
    @Test
    public void testGetConnection() throws IOException {
        Thread thread = new Thread(() -> {
            try (ServerSocket listen = new ServerSocket(PORT_DEFAULT)){
                listen.accept();
            } catch (Exception _) {}
        });
        thread.start();
        Socket socket = CouncilConnection.getConnection(HOST_DEFAULT, PORT_DEFAULT);
        assertNotNull(socket);
        socket.close();
        thread.interrupt();
    }

    /**
     * Tests that a connection attempt to a non-existent server will not block or hang, and that
     * an exception will not be thrown.
     */
    @Test
    public void testGetConnectionNoOtherSide() throws IOException {
        CouncilConnection.getConnection(HOST_DEFAULT, PORT_DEFAULT);
    }

    /**
     * Tests that a message can successfully read and a Message object is returned.
     */
    @Test
    public void testReadMessage() throws IOException, InterruptedException {
        Socket mockedSocket = Mockito.mock(Socket.class);
        Mockito.when(mockedSocket.getInputStream()).thenReturn(new ByteArrayInputStream("MESSAGE 4005:1 _".getBytes()));
        Message message = CouncilConnection.readMessage(mockedSocket);
        assertNotNull(message);
        assertEquals(4005, message.sender().getPort());
        assertEquals(1, message.proposalNum());
        assertEquals("MESSAGE", message.message());
    }


    /**
     * Tests that an IOException is thrown when the readMessage method is never passed input via
     * the socket instead of blocking forever.
     */
    @Test
    public void testReadMessageWontBlock() {
        Assertions.assertThrows(IOException.class, () -> {
            Socket mockedSocket = Mockito.mock(Socket.class);
            // pass an empty input stream to the socket
            Mockito.when(mockedSocket.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
            CouncilConnection.readMessage(mockedSocket);
        });
    }
}
