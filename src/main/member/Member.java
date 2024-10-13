package member;

import message.Message;

import java.io.IOException;
import java.net.Socket;

/**
 * In our Paxos implementation, we have a council of members, each of whom can propose a value, vote for a value,
 * and accept or reject a value. This interface defines the base methods that a member of the Adelaide
 * Suburbs Council can perform. There are 9 members in the council, and 3 of them behave differently
 * from each other and the rest of the members, and so the members will inherit this interface and implement
 * the methods according to their specific behavior. Any member may become council president.
 */
public interface Member {
    void run(); // runs the member.
    void prepare(); // creates a prepare message to broadcast to all councillors.
    Members whoToVoteFor(); // returns the member this member would like to vote for.
    void listenForMessages(); // listens for messages from other councillors.
    void handleMessages(Socket clientSocket); // handles messages from other councillors.
    void promise(Message message, Socket clientSocket) throws IOException; // sends a 'prepare-ok' message to the proposer.
    void acceptRequest(Members toVoteFor); // broadcasts an 'accept-request' message to the majority.
    void accept(Message message, Socket clientSocket) throws IOException; // sends an 'accept-ok' message to the proposer.
    void reject(Message message, Socket clientSocket) throws IOException; // sends an 'accept-reject' message to the proposer.
    void decide(Members president); // broadcasts a 'decide' message to all councillors.

    Members getMemberNumber(); // returns the Members object representing this member.
    void setProposer(boolean proposer); // sets whether this member is a proposer.
    Members whoIsPresident(); // returns the elected president.
}
