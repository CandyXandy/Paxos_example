package member;

import message.Message;
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
    private boolean isProposer; // Whether the member is a proposer or not.
    private final boolean isQuirkMode; // Whether the member is in test mode or not.

    private Members president; // The president of the council. Only decided once the algorithm has run.

    /* must be atomic since it can potentially be accessed concurrently when an acceptor
     how many proposals I have made / the highest proposal number I have seen. */
    private final AtomicInteger proposalNumber;

    public MemberImpl(int memberNumber, boolean isProposer, boolean isTestMode) {
        if (memberNumber < 1 || memberNumber > 9) {
            throw new IllegalArgumentException("Member number must be between 1 and 9.");
        }
        this.memberNumber = Members.getMember(memberNumber);
        this.isProposer = isProposer;
        this.isQuirkMode = isTestMode;
        this.proposalNumber = new AtomicInteger(0);
    }

    public MemberImpl(int memberNumber, boolean isProposer) {
        this(memberNumber, isProposer, false);
    }


    @Override
    public void run() {
        while (president == null) { // While the president has not been decided. We keep running.
            if (isProposer) {
                int retryCount = 0;
                prepare();
                if (president == null) {
                    // exponential backoff if we don't get enough promises
                    retryCount++;
                    backOff(retryCount);
                }
            } else {
                // Acceptors do nothing until they receive a prepare message.
                listenForMessages();
                if (president != null) {
                    // spin once more to make sure that any proposers that are still running can be informed
                    listenForMessages();
                }
            }

        }
        System.out.println(this.getMemberNumber() + " says the president of the council is " + president);
    }


    /**
     * Sends a prepare message to all members of the council. This is the first step in the Paxos algorithm.
     * Increments the proposal number, and then sends a prepare message to all members in the format
     * "PREPARE <member port number>:<proposal number>".
     */
    @Override
    public void prepare() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        proposalNumber.incrementAndGet(); // Increment the proposal number.
        // Use an atomic integer since it will be accessed by multiple threads.
        AtomicInteger promiseCount = new AtomicInteger();
        for (Members member : Members.values()) {
            if (member == this.getMemberNumber()) {
                continue; // Skip myself.
            }
            executorService.submit(() -> sendPrepareMessageToMember(member, promiseCount));
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.onSpinWait(); // Wait for all threads to finish.
        }
        if (promiseCount.get() > Members.values().length / 2) {
            logger.info("Member " + this.getMemberNumber() +
                    " received enough promises to proceed to ACCEPT REQUEST phase for proposal number " +
                    proposalNumber);
            Members presidentVote;
            if (this.president != null) {
                presidentVote = this.president;
            } else {
                presidentVote = whoToVoteFor();
            }
            logger.info("Member " + this.getMemberNumber() + " has voted for " + presidentVote +
                    " in proposal number " + proposalNumber);
            acceptRequest(presidentVote);
        } // else, we didn't get enough promises, so we will try again with a higher proposal number.
    }


    /**
     * Sends a prepare message to the given member of the council. Creates a socket and attempts to connect
     * to the member on the port obtained from the member enum. If the connection is successful, a prepare message
     * is sent to the member in the format "PREPARE <member port number>:<proposal number>". If the member responds
     * with a promise, the promise count is incremented.
     * We ignore any exceptions as failure is expected and doesn't need to be handled, just logged.
     *
     * @param member       : Members : the member to send the prepare message to.
     * @param promiseCount : AtomicInteger : the promise count to increment if the member responds with a promise.
     */
    private void sendPrepareMessageToMember(Members member, AtomicInteger promiseCount) {
        try (Socket socket = CouncilConnection.getConnection(HOST, member.getPort())) {
            // Send the prepare message.
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("PREPARE " + this.getMemberNumber().getPort() + ":" + proposalNumber + " _");
            // wait for the promise
            Message response = CouncilConnection.readMessage(socket);
            if (response.getMessage().startsWith("PREPARE-OK")) {
                // we don't check for proposal number here since an acceptor can promise to a higher proposal number
                if (response.getSender() == this.getMemberNumber()) {
                    if (response.getProposalNum() >= proposalNumber.get()) {
                        // if we get a higher proposal number, or the same proposal number,
                        proposalNumber.set(response.getProposalNum()); // there could already be a president
                        if (response.getValue() != null) {
                            logger.info("Member " + this.getMemberNumber() + " received a promise from " +
                                    member + " for proposal number " + proposalNumber +
                                    " with value " + response.getValue());
                            this.president = response.getValue();
                        }
                    }
                    promiseCount.incrementAndGet(); // Increment the promise count.
                } else {
                    // I've received a promise meant for someone else or out of order, how strange
                    logger.fine("Received a promise from " + member + " with proposal number " +
                            response.getProposalNum() + " which was not what I expected.");
                    // I will ignore this message and move on.
                }
            }
        } catch (IOException e) {
            // not a big deal, log it and move on.
            logger.fine("Connection error to member " + member + ". " + e.getMessage());
        }
    }


    /**
     * This method returns the member that this member would like to vote for. If the member is M1, M2, or M3,
     * then they will vote for themselves. Otherwise, they will randomly vote for one of the 9 members.
     *
     * @return : Members : the member that this member would like to vote for.
     */
    @Override
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


    /**
     * Sets up a server socket to listen for messages from other members of the council. When a message is received,
     * a new thread is created to handle the message.
     * If an exception is thrown, we log the error, but we will propagate back to the run method where we will
     * check if the president has been decided, if not we will end up back here and will begin listening for messages
     * again.
     */
    @Override
    public void listenForMessages() {
        try (
                ServerSocket listenSocket = new ServerSocket(this.getMemberNumber().getPort());
                ExecutorService executorService = Executors.newCachedThreadPool()
        ) {
            listenSocket.setSoTimeout(10000); // Set a timeout of 10 seconds.
            while (this.president == null) {
                Socket clientSocket = listenSocket.accept(); // Wait for a connection.
                executorService.submit(() -> handleMessages(clientSocket));
            }
        } catch (IOException e) {
            logger.fine("Member " + this.getMemberNumber() +
                    "'s Listening Server shut down. " + e.getMessage());
        }
    }


    /**
     * Handles messages received by the member. The message is split into parts, and the first part is the message
     * type. The second part is the proposal number. The message is then handled based on the message type.
     * TODO: quirk mode behaviour
     *
     * @param clientSocket : Socket : The socket that the message was received on.
     */
    @Override
    public void handleMessages(Socket clientSocket) {
        try {
            Message message = CouncilConnection.readMessage(clientSocket);
            switch (message.getMessage()) {
                case "PREPARE":
                    if (message.getProposalNum() > this.proposalNumber.get()) {
                        this.proposalNumber.set(message.getProposalNum());
                        promise(message, clientSocket);
                    } else {
                        logger.fine("member " + this.getMemberNumber() +
                                " received a prepare message with a proposal number " +
                                "less than the current proposal number.");
                    }
                    break;
                case "ACCEPT-REQUEST":
                    if (message.getProposalNum() >= this.proposalNumber.get()) {
                        // TODO - quirk mode behaviour
                        accept(message, clientSocket);
                    } else {
                        reject(message, clientSocket);
                    }
                    break;
                case "DECIDE":
                    if (message.getProposalNum() >= this.proposalNumber.get()) {
                        this.president = message.getValue();
                    } else {
                        logger.fine("Received a decide message with a proposal number less than the current proposal number.");
                    }
                    break;
                default:
                    logger.fine("Unknown message type received: " + message.getMessage());
            }
        } catch (IOException e) {
            logger.fine("Error handling the message. " + e.getMessage());
        }
    }

    /**
     * Sends a promise message to the proposer. This is the second step in the Paxos algorithm.
     * The promise message is sent in the format "PROMISE <member port number>:<proposal number>".
     * If the member has not seen a proposal before, they will send a promise with a value of "_".
     * If the member has already seen a president be elected, they will send a promise with the
     * value of the president.
     *
     * @param message      : Message : The message received from the proposer.
     * @param clientSocket : Socket : The socket that the message was received on.
     */
    @Override
    public void promise(Message message, Socket clientSocket) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        String vote;
        if (this.president == null) {
            vote = "_";
        } else {
            vote = String.valueOf(Members.getMemberNumber(this.president));
        }
        out.println("PREPARE-OK " + message.getSender().getPort() + ":" + message.getProposalNum() + " " + vote);
        out.flush();
        out.close();
    }


    @Override
    public void acceptRequest(Members toVoteFor) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        AtomicInteger acceptCount = new AtomicInteger();
        for (Members member : Members.values()) {
            if (member == this.getMemberNumber()) {
                continue; // Skip myself.
            }
            executorService.submit(() -> sendAcceptRequestToMember(member, toVoteFor, acceptCount));
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.onSpinWait(); // Wait for all threads to finish.
        }
        if (acceptCount.get() > Math.floor((double) Members.values().length / 2)) {
            logger.info("Member " + this.getMemberNumber() + " received enough accepts to decide on " +
                    toVoteFor + " for proposal number " + proposalNumber);
            // We have a majority, so we can decide.
            decide(toVoteFor);
        } // else, we didn't get enough accepts, so we will try again with a higher proposal number.
    }


    private void sendAcceptRequestToMember(Members member, Members toVoteFor, AtomicInteger acceptCount) {
        try (Socket socket = CouncilConnection.getConnection(HOST, member.getPort())) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // send the accept request
            out.println("ACCEPT-REQUEST " + this.getMemberNumber().getPort() + ":" + proposalNumber.get() + " " +
                    Members.getMemberNumber(toVoteFor));
            // read the response
            Message response = CouncilConnection.readMessage(socket);
            if (response.getMessage().startsWith("ACCEPT-OK")) {
                if (response.getProposalNum() == proposalNumber.get() &&
                        response.getSender() == this.getMemberNumber() &&
                        response.getValue() == toVoteFor) {
                    acceptCount.incrementAndGet();
                } else {
                    logger.fine("Received an accept-ok from " + member + " with proposal number " +
                            response.getProposalNum() + " and value " + response.getValue() +
                            " which was not what I expected.");
                }
            } else {
                logger.fine("Received a rejection from " + member + " for the accept request.");
            }
        } catch (IOException e) {
            logger.fine("Member " + this.getMemberNumber() +
                    "experienced a connection error to member " + member + ". " + e.getMessage());
        }
    }


    @Override
    public void accept(Message message, Socket clientSocket) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("ACCEPT-OK " + message.getSender().getPort() + ":" + message.getProposalNum() + " " +
                Members.getMemberNumber(message.getValue()));
        out.flush();
        out.close();
    }

    @Override
    public void reject(Message message, Socket clientSocket) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("ACCEPT-REJECT " + message.getSender().getPort() + ":" + message.getProposalNum() + " _");
        out.flush();
        out.close();
    }


    @Override
    public void decide(Members president) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (Members member : Members.values()) {
            if (member == this.getMemberNumber()) {
                continue; // Skip myself.
            }
            executorService.submit(() -> sendDecideMessageToMember(member, president));
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.onSpinWait(); // Wait for all threads to finish.
        }
        this.president = president;
    }

    private void sendDecideMessageToMember(Members member, Members president) {
        try (Socket socket = CouncilConnection.getConnection(HOST, member.getPort())) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("DECIDE " + this.getMemberNumber().getPort() + ":" + this.proposalNumber.get() + " " +
                    Members.getMemberNumber(president));
            out.flush();
            out.close();
        } catch (IOException e) {
            logger.fine("Connection error to member " + member + ". " + e.getMessage());
        }
    }


    /**
     * Sleeps for a random amount of time depending on the number of retries.
     * This allows us to exponentially back off when retrying, so we don't flood the network when acceptors
     * are busy and reject our proposals. Without this, a proposer could very quickly
     * increase the proposal number and 'cheese' the algorithm.
     *
     * @param retryCount : int : the number of retries that have been attempted.
     */
    private void backOff(int retryCount) {
        long backOffTime = (long) (Math.pow(2, retryCount) * 1000) + new Random().nextInt((100));
        try {
            Thread.sleep(backOffTime);
        } catch (InterruptedException e) {
            logger.fine("Member " + this.getMemberNumber() + " was interrupted while sleeping. " + e.getMessage());
            Thread.currentThread().interrupt();
        }
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
     * Sets the proposer status of the member.
     *
     * @param proposer : boolean : true if the member is a proposer, false otherwise.
     */
    @Override
    public void setProposer(boolean proposer) {
        this.isProposer = proposer;
    }

    /**
     * Returns who has been elected president of the council.
     *
     * @return : Members : the president of the council.
     */
    @Override
    public Members whoIsPresident() {
        return president;
    }
}
