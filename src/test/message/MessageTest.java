package message;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageTest {


    @Test
    public void testMessageComparator() {
        Message m1 = new Message(1, "Message 1");
        Message m2 = new Message(2, "Message 2");

        assertEquals(-1, MessageComparator.compare(m1, m2));
    }

    @Test
    public void testMessageComparator_Greater() {
        Message m1 = new Message(2, "Message 1");
        Message m2 = new Message(1, "Message 2");

        assertEquals(1, MessageComparator.compare(m1, m2));
    }

    @Test
    public void testMessageComparator_Same() {
        Message m1 = new Message(1, "Message 1");
        Message m2 = new Message(1, "Message 2");

        assertEquals(0, MessageComparator.compare(m1, m2));
    }
}
