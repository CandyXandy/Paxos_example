package member;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class MemberTest {
    private final static int DEFAULT_TIMEOUT = 10000; // 10 seconds timeout

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
                String message = in.readLine();
                in.close(); // close the connection, so we don't block the proposer forever
                return message;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Member mem = new MemberImpl(1, true);
        mem.prepare();
        String message = future.get();
        assertTrue(message.contains("PREPARE 4005:1 _"));
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
        List<Future<String>> futures = receivePrepareMessages(numMembers);
        Member mem = new MemberImpl(1, true);
        mem.prepare();
        for (Future<String> future : futures) {
            messages.add(future.get());
        }
        for (String message : messages) {
            assertTrue(message.contains("PREPARE 4005:1 _"));
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
        List<Future<String>> futures = receivePrepareMessages(numMembers);
        Member mem = new MemberImpl(1, true);
        mem.prepare();
        for (Future<String> future : futures) {
            messages.add(future.get());
        }
        for (String message : messages) {
            assertTrue(message.contains("PREPARE 4005:1 _"));
            System.out.println("Message received: " + message);
        }
    }


    /**
     * Tests that an acceptor will return a prepare-ok message when it receives a prepare message.
     */
    @Test
    public void promiseTest() throws IOException {
        // mock the socket that is given to the acceptor's handleMessage method
        Socket socket = Mockito.mock(Socket.class);
        Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream("PREPARE 4005:1 _".getBytes()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Mockito.when(socket.getOutputStream()).thenReturn(out);
        Member mem = new MemberImpl(2, false);
        mem.handleMessages(socket);
        Mockito.verify(socket).getInputStream();
        // verify the mocked socket is sending the correct message
        assertEquals("PREPARE-OK 4005:1 _".strip(), out.toString().strip());
    }


    /**
     * Tests that members 1 - 3 will vote for themselves, and other members will vote for anyone.
     */
    @Test
    public void testVoteFor() {
        Member m1 = new MemberImpl(1, true);
        assertEquals(Members.M1, m1.whoToVoteFor());
        Member m2 = new MemberImpl(2, true);
        assertEquals(Members.M2, m2.whoToVoteFor());
        Member m3 = new MemberImpl(3, true);
        assertEquals(Members.M3, m3.whoToVoteFor());
        Member m4 = new MemberImpl(4, true);
        Members vote = m4.whoToVoteFor();
        assertNotNull(vote);
    }

    @Test
    public void testPaxosTwoProposersNoQuirksImmediateResponse() {
        List<Member> members = getAllMembers();
        ExecutorService executor = Executors.newCachedThreadPool();
        // set the first two members to be proposers
        members.get(0).setProposer(true);
        members.get(1).setProposer(true);
        // start all the members
        for (Member member : members) {
            executor.submit(new Thread(member::run));
        }
        // wait for the members to finish
        executor.shutdown();
        while (!executor.isTerminated()) Thread.onSpinWait();
        // count up the votes for each member
        int[] votes = new int[Members.values().length];
        for (Member member : members) {
            votes[Members.getMemberNumber(member.whoIsPresident())]++;
        }
        // check that everyone voted for the same president
        int maxVotes = 0;
        for (int vote : votes) {
            if (vote > maxVotes) {
                maxVotes = vote;
            }
        }
        assertEquals(maxVotes, members.size());
    }


    /**
     * Tests the case where all M1-M9 have immediate responses to voting queries.
     */
    @Test
    public void testPaxosNoQuirksImmediateResponse() {
        List<Member> members = getAllMembers();
        ExecutorService executor = Executors.newCachedThreadPool();
        // set the first member to be the proposer
        members.getFirst().setProposer(true);
        // start all the members
        for (Member member : members) {
            executor.submit(new Thread(member::run));
        }
        // wait for the members to finish
        executor.shutdown();
        while (!executor.isTerminated()) Thread.onSpinWait();
        // check that all members have the same president
        for (Member member : members) {
            assertEquals(Members.M1, member.whoIsPresident());
        }
    }



    /* HELPERS */

    /**
     * Instantiates all members of the council
     * Members are instantiated as acceptors, and they are not in quirk mode by default
     *
     * @return : List<Member> : a list of all members of the council.
     */
    private List<Member> getAllMembers() {
        List<Member> members = new ArrayList<>();
        for (Members member : Members.values()) {
            members.add(new MemberImpl(Members.getMemberNumber(member), false));
        }
        return members;
    }


    /**
     * Creates a list of future that will receive prepare message as the given number of members.
     * The list of members sending a message will always start from member 2, and increase by 1 for each member.
     *
     * @param numMembers : int : the number of members that are to receive a prepare message.
     * @return : List<Future<String>> : a list of futures that will return the message received by each member.
     */
    private List<Future<String>> receivePrepareMessages(int numMembers) {
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<String>> futures = new ArrayList<>();
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
        return futures;
    }
}
