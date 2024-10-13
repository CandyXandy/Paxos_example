package member;

import member.quirk.Quirk;
import member.quirk.QuirkM1;
import member.quirk.QuirkM2;
import member.quirk.QuirkM3;
import member.quirk.QuirkOther;

import message.Message;
import util.CouncilConnection;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * The implementation of the Member interface. This class represents a member of the Adelaide Suburbs Council.
 * Each member can propose a value, vote for a value, and accept or reject a value. There are 9 members in the council,
 * and 3 of them behave differently from each other and the rest of the members.
 * Members act according to whether they're a proposer or an acceptor, and they can run the  Paxos algorithm to
 * elect a president of the council.
 */
public class MemberImpl implements Member {
    private final static Logger logger = Logger.getLogger(MemberImpl.class.getName());

    private final String HOST = "localhost"; // The host to connect to.

    private final Members memberNumber; // The number of the member in the council.
    private boolean isProposer; // Whether the member is a proposer or not.
    private final Quirk myQuirks; // This member's quirks, if they have any.

    private Members president; // The president of the council. Only decided once the algorithm has run.

    private boolean finish = false; // Whether we are confident the president has been decided or not.

    /* must be atomic since it can potentially be accessed concurrently when an acceptor
     how many proposals I have made / the highest proposal number I have seen. */
    private final AtomicInteger proposalNumber;

    /**
     * Constructor for the MemberImpl class. The constructor takes the member number and whether the member is a proposer.
     * The constructor also takes a boolean to determine if the member is in test mode or not.
     *
     * @param memberNumber : int : the number of the member in the council.
     * @param isProposer : boolean : true if the member is a proposer, false otherwise.
     * @param isTestMode : boolean : true if the member is in test mode, false otherwise.
     */
    public MemberImpl(int memberNumber, boolean isProposer, boolean isTestMode) {
        if (memberNumber < 1 || memberNumber > 9) {
            throw new IllegalArgumentException("Member number must be between 1 and 9.");
        }
        this.memberNumber = Members.getMember(memberNumber);
        this.isProposer = isProposer;
        this.proposalNumber = new AtomicInteger(0);
        if (isTestMode) { // If the member is in test mode, they will have quirks.
            this.myQuirks = whoseQuirks();
        } else { // If the member is not in test mode, they will not have quirks.
            this.myQuirks = null;
        }
    }

    /**
     * 'Default' constructor, takes a member number to decide which member we are, and whether we're
     * a proposer. This constructor is used when the member is not in test mode.
     * @param memberNumber : int : the number of the member in the council.
     * @param isProposer : boolean : true if the member is a proposer, false otherwise.
     */
    public MemberImpl(int memberNumber, boolean isProposer) {
        this(memberNumber, isProposer, false);
    }


    /**
     * Begin participating in the paxos algorithm, if the member is a proposer, they will try sending a prepare
     * message to acceptors, and exponentially backing off when they fail to avoid quickly increasing their
     * proposal number and 'gaming' the algorithm.
     * If the member is an acceptor, they just listen out for messages until they're sure
     * a president has been decided. This will only happen if they have a value for president, and they haven't
     * received any messages from a proposer in a while.
     * If a proposer has attempted to make a proposal 10 times and failed, they will become an acceptor.
     * However, to avoid making progress impossible, acceptors will also become proposers if they haven't received
     * a message in a very long time.
     * This method will run until a president has been decided, and the finish flag is set to true, which will only
     * happen when a proposer has received enough promises with the same value to form a majority, or when an acceptor
     * hasn't received a message in a long time, and they have a value for president.
     */
    @Override
    public void run() {
        int retryCount = 0;
        while (!finish) { // Unless we're absolutely confident everyone has decided on a president, keep going.
            if (isProposer) {
                prepare();
                if (president == null) {
                    if (retryCount >= 10) {
                        // tried 10 times and still can't succeed, there might be too many proposers
                        logger.info("Member " + this.getMemberNumber() + " has tried 10 times and failed to" +
                                " get enough votes, becoming an acceptor.");
                        this.setProposer(false); // I will become an acceptor.
                        continue;
                    }
                    // exponential backoff if we don't get enough promises
                    retryCount++;
                    backOff(retryCount);
                }
            } else {
                // Acceptors do nothing until they receive a prepare message.
                listenForMessages();
            }
        }
    }


    /**
     * Sends a prepare message to all members of the council asynchronously using an ExecutorService to
     * manage the thread pools. Once all the messages have been sent, successful or not, we check if we have
     * received enough promises to proceed to the accept-request phase. If we have, we vote for a president
     * and send an accept-request message to all members of the council.
     * If we receive enough promises with the same value, we can assume that value is the president, and
     * we can output it to the console.
     * If we don't receive enough promises, we will try again with a higher proposal number.
     */
    @Override
    public void prepare() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        proposalNumber.incrementAndGet(); // Increment the proposal number.
        logger.info("Member " + this.getMemberNumber() + " is preparing for proposal number " + proposalNumber);
        // Use an atomic integer since it will be accessed by multiple threads.
        AtomicInteger promiseCount = new AtomicInteger();
        ConcurrentHashMap<Members, Members> promiseValues = new ConcurrentHashMap<>();
        for (Members member : Members.values()) {
            if (member == this.getMemberNumber()) {
                continue; // Skip myself.
            }
            executorService.submit(() -> sendPrepareMessageToMember(member, promiseCount, promiseValues));
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.onSpinWait(); // Wait for all threads to finish.
        }
        if (promiseCount.get() > Members.values().length / 2) { // if we have a majority of promises

            // if we got enough promises with a value to potentially form a majority
            if (promiseValues.size() > Math.floor((double) Members.values().length / 2)) {
                // check if we have a majority of promises with the same value
                if (checkPromisesForMajority(promiseValues)) {
                    // log the president to the info level
                    logger.info("Member " + this.getMemberNumber() + " received a majority of" +
                            " promises with value " + president + " for proposal number " + proposalNumber);
                    // Output the president to the console.
                    System.out.println("Member " + this.getMemberNumber() + " says " +
                            this.president + " is the president.");
                    this.finish = true; // we are confident the president has been decided.
                    return; // we can exit the algorithm.
                }
            }

            logger.info("Member " + this.getMemberNumber() +
                    " received enough promises to proceed to ACCEPT REQUEST phase for proposal number " +
                    proposalNumber);
            // choose who to vote for
            Members presidentVote;
            if (this.president != null) {
                presidentVote = this.president;
            } else {
                presidentVote = whoToVoteFor();
            }
            logger.info("Member " + this.getMemberNumber() + " has voted for " + presidentVote +
                    " in proposal number " + proposalNumber);
            acceptRequest(presidentVote); // proceed to the accept-request phase.
        }
        // else, we didn't get enough promises, so we will try again with a higher proposal number.
    }


    /**
     * Checks the promises received from the acceptors to see if there is a majority with the same value.
     * This method is only called when enough promises have been received to form a majority, so we need
     * to check if the majority of promises have the same value. If they do, we can assume that value is the
     * president, and so we return true.
     *
     * @param promiseValues : ConcurrentHashMap<Members, Members> : the promises received from the acceptors.
     * @return : boolean : true if a majority of promises have the same value, false otherwise.
     */
    private boolean checkPromisesForMajority(ConcurrentHashMap<Members, Members> promiseValues) {
        int[] votes = new int[Members.values().length]; // Array to store the votes.
        for (Members vote : promiseValues.values()) { // count the votes, each index in the array represents a member.
            votes[Members.getMemberNumber(vote) - 1]++;
        }
        int maxVotes = 0;
        Members voteLeader = null;
        for (int i = 0; i < votes.length; i++) {
            if (votes[i] > maxVotes) {
                maxVotes = votes[i];
                voteLeader = Members.getMember(i + 1);
            }
        }
        if (voteLeader == null) {
            return false; // only happens if no promises were received, which should never happen.
        }
        if (maxVotes > Math.floor((double) Members.values().length / 2)) {
            // if the vote leader has a majority of votes, we can assume they are the president.
            president = voteLeader;
            // send a decide message to all members in case any of them missed the majority like we did.
            decide(president);
            return true;
        }
        return false;
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
    private void sendPrepareMessageToMember(Members member, AtomicInteger promiseCount,
                                            ConcurrentHashMap<Members, Members> promiseValues) {
        try (Socket socket = CouncilConnection.getConnection(HOST, member.getPort())) {
            // Send the prepare message.
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("PREPARE " + this.getMemberNumber().getPort() + ":" + proposalNumber + " _");
            if (myQuirks != null) {
                myQuirks.rollDice(socket); // roll the dice to determine the member's behavior.
            }
            // wait for the promise
            Message response = CouncilConnection.readMessage(socket);
            if (response.message().startsWith("PREPARE-OK")) {
                // don't check for proposal number, an acceptor can make a promise to a higher proposal number.
                if (response.sender() == this.getMemberNumber()) {
                    if (response.proposalNum() >= proposalNumber.get()) {
                        // if we get a higher proposal number, or the same proposal number,
                        proposalNumber.set(response.proposalNum()); // there could already be a president
                        if (response.value() != null) {
                            logger.fine("Member " + this.getMemberNumber() + " received a promise from " +
                                    member + " for proposal number " + proposalNumber +
                                    " with value " + response.value());
                            this.president = response.value();
                            promiseValues.put(member, response.value());
                        }
                    }
                    promiseCount.incrementAndGet(); // Increment the promise count.
                } else {
                    // I've received a promise meant for someone else or out of order, how strange
                    logger.fine("Received a promise from " + member + " with proposal number " +
                            response.proposalNum() + " which was not what I expected.");
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
     * The server socket has a timeout of between 0 and 30 seconds, so if we haven't received a message in a long time,
     * we can assume that the proposers have failed, and one of the acceptors will have to take over.
     * We randomise the timeout on this so that all acceptors don't suddenly all become proposers at the same time,
     * making progress impossible.
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
            int attemptsWithoutMessage = 0; // Keep track of how many times we have not received a message.
            listenSocket.setSoTimeout(new Random().nextInt(1, 4) * 10000); // timeout between 10 and 30 sec
            while (attemptsWithoutMessage <= 3) { // If we haven't received a message in 3 attempts, we will shut down.
                try {
                Socket clientSocket = listenSocket.accept(); // Wait for a connection.
                executorService.submit(() -> handleMessages(clientSocket));
                attemptsWithoutMessage = 0; // Reset the number of attempts if we receive a message.
                } catch (SocketTimeoutException e) {
                    attemptsWithoutMessage++; // increment the number of attempts on timeout.
                }
            }
            // check if there is a president
            if (this.president == null) {
                /* if we haven't received a message in 30 seconds, and we don't have a president, we can assume
                    that the proposers have failed, one of the acceptors will have to take over. */
                logger.info("Member " + this.getMemberNumber() + " has not received a message in a long time, " +
                        "assuming proposer failure, and taking over.");
                this.setProposer(true); // I will take over as a proposer.
                return;
            }
            // if we have a president, we can exit the algorithm.
            System.out.println("Member " + this.getMemberNumber() + " says " + this.president + " is the president.");
            this.finish = true; // we are confident the president has been decided, so we can exit the algorithm.
        } catch (IOException e) {
            logger.fine("Member " + this.getMemberNumber() +
                    "'s Listening Server shut down. " + e.getMessage());
        }
    }


    /**
     * Handles messages received by the member. The message is split into parts, and the first part is the message
     * type. The second part is the proposal number. The message is then handled based on the message type.
     *
     * @param clientSocket : Socket : The socket that the message was received on.
     */
    @Override
    public void handleMessages(Socket clientSocket) {
        try {
            if (myQuirks != null) { // if in quirk mode
                myQuirks.rollDice(clientSocket); // roll the dice to determine the member's behavior.
            }
            Message message = CouncilConnection.readMessage(clientSocket);
            switch (message.message()) {
                case "PREPARE":
                    if (message.proposalNum() > this.proposalNumber.get()) {
                        this.proposalNumber.set(message.proposalNum());
                        promise(message, clientSocket);
                    } else {
                        logger.fine("member " + this.getMemberNumber() +
                                " received a prepare message with a proposal number " +
                                "less than the current proposal number.");
                    }
                    break;
                case "ACCEPT-REQUEST":
                    if (message.proposalNum() >= this.proposalNumber.get()) {
                        accept(message, clientSocket);
                    } else {
                        reject(message, clientSocket);
                    }
                    break;
                case "DECIDE":
                    if (message.proposalNum() >= this.proposalNumber.get()) {
                        this.president = message.value();
                    } else {
                        logger.fine("Received a decide message with a proposal number" +
                                " less than the current proposal number.");
                    }
                    break;
                default:
                    logger.fine("Unknown message type received: " + message.message());
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
        out.println("PREPARE-OK " + message.sender().getPort() + ":" + message.proposalNum() + " " + vote);
        out.flush();
        out.close();
    }


    /**
     * Broadcasts an accept-request message to all members of the council asynchronously. If the proposer receives
     * enough accept-ok messages to form a majority, they will decide on the value they voted for.
     *
     * @param toVoteFor : Members : the member that this member would like to vote for.
     */
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


    /**
     * Sends the accept-request message to the given member of the council. Creates a socket and attempts to connect
     * to the member on the port obtained from the member enum. If the connection is successful, the message is sent
     * and the response is read from the member. If the member responds with an accept-ok message, the accept count
     * passed as an argument is incremented.
     * We ignore any exceptions or failures, just logging them to the debug level log.
     *
     * @param member : Members : the member to send the accept-request message to.
     * @param toVoteFor : Members : the member that this member would like to vote for.
     * @param acceptCount : AtomicInteger : the accept count to increment if the member responds with an accept-ok.
     */
    private void sendAcceptRequestToMember(Members member, Members toVoteFor, AtomicInteger acceptCount) {
        try (Socket socket = CouncilConnection.getConnection(HOST, member.getPort())) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // send the accept request
            out.println("ACCEPT-REQUEST " + this.getMemberNumber().getPort() + ":" + proposalNumber.get() + " " +
                    Members.getMemberNumber(toVoteFor));
            if (myQuirks != null) { // if in quirk mode
                myQuirks.rollDice(socket); // roll the dice to determine the member's behavior.
            }
            // read the response
            Message response = CouncilConnection.readMessage(socket);
            if (response.message().startsWith("ACCEPT-OK")) {
                if (response.proposalNum() == proposalNumber.get() &&
                        response.sender() == this.getMemberNumber() &&
                        response.value() == toVoteFor) {
                    acceptCount.incrementAndGet();
                } else {
                    logger.fine("Received an accept-ok from " + member + " with proposal number " +
                            response.proposalNum() + " and value " + response.value() +
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


    /**
     * Sends an accept-ok message to the proposer at the other end of the client socket input argument.
     *
     * @param message : Message : the message received from the proposer.
     * @param clientSocket : Socket : the socket that the message was received on.
     * @throws IOException : if the accept-ok message could not be sent to the proposer.
     */
    @Override
    public void accept(Message message, Socket clientSocket) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        // ACCEPT-OK sendersPort:proposalNumber value
        out.println("ACCEPT-OK " + message.sender().getPort() + ":" + message.proposalNum() + " " +
                Members.getMemberNumber(message.value()));
        out.flush();
        out.close();
    }


    /**
     * Sends a reject message to the proposer at the other end of the client socket input argument.
     *
     * @param message : Message : the message received from the proposer.
     * @param clientSocket : Socket : the socket that the message was received on.
     * @throws IOException : if the reject message could not be sent to the proposer.
     */
    @Override
    public void reject(Message message, Socket clientSocket) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        // ACCEPT-REJECT port:proposalNumber _
        out.println("ACCEPT-REJECT " + message.sender().getPort() + ":" + message.proposalNum() + " _");
        out.flush();
        out.close();
    }


    /**
     * Creates an ExecutorService to asynchronously send a decide message to all members of the council.
     * Once all the messages have been sent, successful or not, we set the president field for this member
     * to the input argument. However, this doesn't necessarily guarantee that this value is the elected
     * president as another proposer may have sent a prepare message with a higher proposal number in the time
     * it took for this proposer to enter this function after receiving enough accept-ok messages.
     *
     * @param president : Members : the member that the proposer has voted for as president.
     */
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


    /**
     * Creates a socket connection to the given member and sends a decide message to the member with
     * the input Member as the proposer's vote for president.
     *
     * @param member : Members : the member to send the decide message to.
     * @param president : Members : the member that the proposer has voted for as president.
     */
    private void sendDecideMessageToMember(Members member, Members president) {
        try (Socket socket = CouncilConnection.getConnection(HOST, member.getPort())) {
            if (myQuirks != null) { // if in quirk mode
                myQuirks.rollDice(socket); // roll the dice to determine the member's behavior.
            }
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // DECIDE port:proposalNumber value
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
            logger.fine("Member " + this.getMemberNumber() +
                    " was interrupted while sleeping. " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Chooses which quirks the member has based on their member number.
     *
     * @return : Quirk : the quirks of the member.
     */
    private Quirk whoseQuirks() {
        return switch (this.getMemberNumber()) {
            case M1 -> new QuirkM1();
            case M2 -> new QuirkM2();
            case M3 -> new QuirkM3();
            default -> new QuirkOther();
        };
    }


    /**
     * Gets this member's quirks.
     *
     * @return : Quirk : the quirks of the member.
     */
    @Override
    public Quirk getMyQuirks() {
        return myQuirks;
    }


    /**
     * This method returns the number of the member in the council.
     *
     * @return the number of the member in the council.
     */
    @Override
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
