package member.quirk;


import java.net.Socket;
import java.util.Random;
import java.util.logging.Logger;

/**
 * The M1 member's quirks are that they are very chatty over social media, and responds to messages almost
 * instantly, as if they had an in-brain connection to their mobile phone. They are very reliable. They never
 * drop connections, and they delay their responses by up to a second.
 */
public class QuirkM1 implements Quirk {

    /**
     * M1 will delay their response time by up to a second by sleeping for that long.
     */
    private void delay() {
        try {
            Thread.sleep(new Random().nextInt(1000)); // M1 will sleep for a maximum of a second.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * M1 should never have setDelayForm called on them, as they will always respond almost instantly.
     * It won't throw an exception, but it does nothing.
     *
     * @param delayForm : int : the delay form to ignore for M1.
     */
    @Override
    public void setDelayForm(int delayForm) {
        // it doesn't matter what the delay form is for M1, as they will always respond almost instantly.
    }

    /**
     * M1 will roll a die to determine if they should delay their response time. If the dice roll is 1 or 2,
     * they will delay their response time by up to a second.
     */
    @Override
    public void rollDice(Socket connection) {
        int diceRoll = new Random().nextInt(6) + 1;
        if (diceRoll == 1 || diceRoll == 2) { // 1/3 chance of delaying the message for up to a second
            Logger.getLogger(QuirkM1.class.getName()).fine("M1 is delaying their response.");
            delay();
        }
    }
}
