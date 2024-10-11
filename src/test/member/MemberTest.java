package member;

import org.junit.jupiter.api.Test;
import util.CouncilConnection;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MemberTest {
    private final static int DEFAULT_TIMEOUT = 10000; // 10 seconds timeout
    private final static String HOST = "localhost";

    /**
     * Test that the member number is forced to be between 1 and 9.
     */
    @Test
    public void invalidMemberNumber() {
        assertThrows(IllegalArgumentException.class, () -> new MemberImpl(0, true));
    }

    @Test
    public void invalidMemberNumber2() {
        assertThrows(IllegalArgumentException.class, () -> new MemberImpl(10, true));
    }

    @Test
    public void invalidMemberNumber3() {
        assertThrows(IllegalArgumentException.class, () -> new MemberImpl(-1, true));
    }

    /**
     * Tests that a member can be created with a valid member number.
     */
    @Test
    public void validMemberNumber() {
        new MemberImpl(1, true);
    }


    /**
     * Tests that a member set to proposer will try and connect to other members and
     * send a prepare message in the correct format.
     */
    @Test
    public void testPrepare() throws ExecutionException, InterruptedException {
        Future<String> future = Executors.newSingleThreadExecutor().submit(() -> {
            try (ServerSocket socket = new ServerSocket(Members.M2.getPort())) {
                socket.setSoTimeout(DEFAULT_TIMEOUT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.accept().getInputStream()));
                String message =  in.readLine();
                in.close(); // close the connection, so we don't block the proposer forever
                return message;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Member mem = new MemberImpl(1, true);
        mem.prepare();
        String message = future.get();
        assertTrue(message.contains("PREPARE 4005:1"));
        System.out.println("Message received: " + message);
    }


    /**
     * Tests that a member set to proposer will try and connect to multiple other members and
     * send a prepare message in the correct format.
     */
    @Test
    public void testMultiPrepare() throws ExecutionException, InterruptedException {
        int numMembers = 2;
        List<String> messages = new ArrayList<>();
        List<Future<String>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        for (int i = 0; i < numMembers; i++) {
            int finalI = i;
            Future<String> future = executor.submit(() -> {
                try (ServerSocket socket = new ServerSocket(Members.getMember(finalI + 2).getPort())) {
                    socket.setSoTimeout(DEFAULT_TIMEOUT);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.accept().getInputStream()));
                    return in.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }
        Member mem = new MemberImpl(1, true);
        mem.prepare();
        for (Future<String> future : futures) {
            messages.add(future.get());
        }
        for (String message : messages) {
            assertTrue(message.contains("PREPARE 4005:1"));
            System.out.println("Message received: " + message);
        }
    }


    /**
     * Tests that a member set to proposer will try and connect to multiple other members and
     * send a prepare message to all 8 other members in the correct format.
     */
    @Test
    public void testMultiPrepareAll8() throws ExecutionException, InterruptedException {
        int numMembers = 8;
        List<String> messages = new ArrayList<>();
        List<Future<String>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = 0; i < numMembers; i++) {
            int finalI = i;
            Future<String> future = executor.submit(() -> {
                try (ServerSocket socket = new ServerSocket(Members.getMember(finalI + 2).getPort())) {
                    socket.setSoTimeout(DEFAULT_TIMEOUT);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.accept().getInputStream()));
                    String message = in.readLine();
                    in.close(); // close the connection, so we don't block the proposer forever
                    return message;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }
        Member mem = new MemberImpl(1, true);
        mem.prepare();
        for (Future<String> future : futures) {
            messages.add(future.get());
        }
        for (String message : messages) {
            assertTrue(message.contains("PREPARE 4005:1"));
            System.out.println("Message received: " + message);
        }
    }


    /**
     * Tests that a member set to acceptor will return a promise message to a proposer.
     */
    @Test
    public void testPromise() {
        Member mem = new MemberImpl(1, false);
        Executors.newSingleThreadExecutor().submit(mem::listenForMessages);
        String message;
        try (
                Socket socket = CouncilConnection.getConnection(HOST, Members.M1.getPort());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("PREPARE " + Members.M2.getPort() + ":1");
            message = in.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertTrue(message.startsWith("PREPARE-OK " + Members.M1.getPort() + ":1"));
        System.out.println("Message received: " + message);
    }
}
