package member;

import util.CouncilConnection;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class MemberImpl implements Member {
    private final static Logger logger = Logger.getLogger(MemberImpl.class.getName());

    private final String HOST = "localhost";

    private final Members memberNumber; // The number of the member in the council.
    private final boolean isProposer; // Whether the member is a proposer or not.
    private final boolean isQuirkMode; // Whether the member is in test mode or not.

    private Members president; // The president of the council. Only decided once the algorithm has run.

    private int proposalNumber; // how many proposals I have made / the highest proposal number I have seen.

    public MemberImpl(int memberNumber, boolean isProposer, boolean isTestMode) {
        if (memberNumber < 1 || memberNumber > 9) {
            throw new IllegalArgumentException("Member number must be between 1 and 9.");
        }
        this.memberNumber = Members.getMember(memberNumber);
        this.isProposer = isProposer;
        this.isQuirkMode = isTestMode;
    }

    public MemberImpl(int memberNumber, boolean isProposer) {
        this(memberNumber, isProposer, false);
    }


    @Override
    public void run() {
        while (president == null) { // While the president has not been decided. We keep running.
            if (isProposer) {
                prepare();
            } else {
                // Acceptors do nothing until they receive a prepare message.
                listenForMessages();
            }
        }
    }


    /**
     * Sends a prepare message to all members of the council. This is the first step in the Paxos algorithm.
     * Increments the proposal number, and then sends a prepare message to all members in the format
     * "PREPARE <member port number>:<proposal number>".
     */
    @Override
    public void prepare() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        proposalNumber++; // Increment the proposal number.
        AtomicInteger promiseCount = new AtomicInteger(); // Use an atomic integer since it will be accessed by multiple threads.
        for (Members member : Members.values()) {
            if (member == this.getMemberNumber()) {
                continue; // Skip myself.
            }
            executorService.submit(() -> {
                try (Socket socket = CouncilConnection.getConnection(HOST, member.getPort())) {
                    // Send the prepare message.
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("PREPARE " + this.getMemberNumber().getPort() + ":" + proposalNumber);
                    // wait for the promise
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String response = in.readLine();
                    if (response.contains("PREPARE-OK")) {
                        promiseCount.incrementAndGet(); // Increment the promise count.
                    }
                } catch (IOException e) {
                    // not a big deal, log it and move on.
                    logger.warning("Connection error to member " + member + ". " + e.getMessage());
                }
            });
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {/* Wait for all threads to finish*/}
        if (promiseCount.get() > Members.values().length / 2) {
            // TODO: ACCEPT VALUES FROM ACCEPTORS AND FACTOR THAT INTO THE DECISION.
            Members presidentVote = whoToVoteFor();
            acceptRequest(presidentVote, proposalNumber);
        }
    }


    /**
     * This method returns the member that this member would like to vote for. If the member is M1, M2, or M3,
     * then they will vote for themselves. Otherwise, they will randomly vote for one of the 9 members.
     *
     * @return : Members : the member that this member would like to vote for.
     */
    public Members whoToVoteFor() {
        switch (this.getMemberNumber()) {
            case M1:
                return Members.M1;
            case M2:
                return Members.M2;
            case M3:
                return Members.M3;
            case M4:
            case M5:
            case M6:
            case M7:
            case M8:
            case M9:
                // randomly vote for one of the 9 members.
                Random random = new Random();
                int randomMember = random.nextInt(9) + 1;
                return Members.getMember(randomMember);
        }
        return null; // Should never reach here.
    }


    @Override
    public void listenForMessages() {
        try (
                ServerSocket listenSocket = new ServerSocket(this.getMemberNumber().getPort());
                ExecutorService executorService = Executors.newCachedThreadPool()
        ) {
            while (this.president == null) {
                Socket clientSocket = listenSocket.accept(); // Wait for a connection.
                executorService.submit(() -> handleMessages(clientSocket));
            }
        } catch (IOException e) {
            logger.warning("Listening Server shut down unexpectedly. " + e.getMessage());
        }
    }


    /**
     * Handles messages received by the member. The message is split into parts, and the first part is the message type.
     * The second part is the proposal number. The message is then handled based on the message type.
     *
     * @param clientSocket : Socket : The socket that the message was received on.
     */
    @Override
    public void handleMessages(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String message = in.readLine();
            String[] parts = message.split(" ");
            String messageType = parts[0];
            String[] proposalParts = parts[1].split(":");
            Members sender = Members.getMember(Integer.parseInt(proposalParts[1]));
            int proposalNum = Integer.parseInt(proposalParts[1]);
            switch (messageType) {
                case "PREPARE":
                    if (proposalNum > this.proposalNumber) {
                        this.proposalNumber = proposalNum;
                        promise(sender, proposalNum, in, out);
                    } else {
                        clientSocket.close(); // just ignore the message and kill the connection.
                    }
                    break;
                case "ACCEPT-REQUEST":
                    accept(proposalNum);
                    break;
                case "DECIDE":
                    decide(proposalNum);
                    break;
                default:
                    logger.warning("Unknown message type received: " + messageType);
            }

        } catch (IOException e) {
            logger.warning("Error handling the message. " + e.getMessage());
        }
    }

    /**
     * Sends a promise message to the proposer. This is the second step in the Paxos algorithm.
     * The promise message is sent in the format "PROMISE <member port number>:<proposal number>".
     *
     * @param sender      : Members : The member who sent the prepare message.
     * @param proposalNum : int : The proposal number received in the prepare message.
     * @param in          : BufferedReader : The input stream from the proposer.
     * @param out         : PrintWriter : The output stream to the proposer.
     */
    @Override
    public void promise(Members sender, int proposalNum, BufferedReader in, PrintWriter out) {
        out.write("PREPARE-OK " + this.getMemberNumber().getPort() + ":" + proposalNum);
        out.flush();
        out.close();
    }

    @Override
    public void acceptRequest(Members toVoteFor, int proposalNum) {

    }

    @Override
    public void accept(int proposalNum) {

    }

    @Override
    public void reject(int proposalNum) {

    }

    @Override
    public void decide(int proposalNum) {

    }


    /**
     * This method returns the number of the member in the council.
     *
     * @return the number of the member in the council.
     */
    public Members getMemberNumber() {
        return memberNumber;
    }


    /**
     * This method returns whether the member is a proposer or not.
     *
     * @return true if the member is a proposer, false otherwise.
     */
    public boolean isProposer() {
        return isProposer;
    }

    /**
     * This method returns whether the member is in test mode or not.
     *
     * @return true if the member is in test mode, false otherwise.
     */
    public boolean isQuirkMode() {
        return isQuirkMode;
    }
}
