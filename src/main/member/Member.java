package member;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * In our Paxos implementation, we have a council of members, each of whom can propose a value, vote for a value,
 * and accept or reject a value. This interface defines the base methods that a member of the Adelaide
 * Suburbs Council can perform. There are 9 members in the council, and 3 of them behave differently
 * from each other and the rest of the members, and so the members will inherit this interface and implement
 * the methods according to their specific behavior. Any member may become council president.
 */
public interface Member {
    public void run(); // runs the member.
    public void prepare(); // creates a prepare message to broadcast to all councillors.
    Members whoToVoteFor(); // returns the member this member would like to vote for.
    public void listenForMessages(); // listens for messages from other councillors.
    public void handleMessages(Socket clientSocket); // handles messages from other councillors.
    public void promise(Members sender, int proposalNum, BufferedReader in, PrintWriter out); // sends a 'prepare-ok' message to the proposer.
    public void acceptRequest(Members toVoteFor, int proposalNum); // broadcasts an 'accept-request' message to the majority.
    public void accept(int proposalNum); // sends an 'accept-ok' message to the proposer.
    public void reject(int proposalNum); // sends an 'accept-reject' message to the proposer.
    public void decide(int proposalNum); // broadcasts a 'decide' message to all councillors.
}
