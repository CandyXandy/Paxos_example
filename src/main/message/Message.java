package message;

import member.Members;

/**
 * This class represents a message that is sent between members of the Adelaide Suburbs Council.
 * The message contains a proposal number and a message, the sender of the message, and the value
 * that the sender is proposing.
 */
public record Message(int proposalNum, Members sender, String message, Members value) {
}
