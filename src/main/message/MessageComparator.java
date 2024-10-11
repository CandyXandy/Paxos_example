package message;

/**
 * This class compares two messages based on their proposal numbers.
 */
public class MessageComparator {
    public static int compare(Message m1, Message m2) {
        return Integer.compare(m1.getProposalNum(), m2.getProposalNum());
    }
}
