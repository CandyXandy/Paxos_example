package message;

/**
 * This class represents a message that is sent between members of the Adelaide Suburbs Council.
 * The message contains a proposal number and a message.
 */
public class Message {
    private final int proposalNum;
    private final String message;

        public Message(int proposalNum, String message) {
            this.proposalNum = proposalNum;
            this.message = message;
        }

        public int getProposalNum() {
            return proposalNum;
        }

        public String getMessage() {
            return message;
        }
}
