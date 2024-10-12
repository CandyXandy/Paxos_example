package message;

import member.Members;

/**
 * This class represents a message that is sent between members of the Adelaide Suburbs Council.
 * The message contains a proposal number and a message.
 */
public class Message {
    private final int proposalNum;
    private final Members sender;
    private final String message;
    private final Members value;

        public Message(int proposalNum, Members sender, String message, Members value) {
            this.sender = sender;
            this.proposalNum = proposalNum;
            this.message = message;
            this.value = value;
        }

        public int getProposalNum() {
            return proposalNum;
        }

        public String getMessage() {
            return message;
        }

        public Members getSender() {
            return sender;
        }

        public Members getValue() {
            return value;
        }
}
