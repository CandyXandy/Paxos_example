package member.quirk;

import java.net.Socket;


/**
 * M3 is not as responsive as M1, but is more reliable than M2, and will generally be somewhat reliable, but
 * they may occasionally decide to go camping, where they will be completely unresponsive, and drop all
 * connections for a minute.
 */
public class QuirkM3 implements Quirk {

    private int delayForm;


    /**
     * M3 will delay their response time by up to 30 seconds, depending on the delay form.
     */
    private void delay() {
        switch (delayForm) {
            case 0:
                return;
            case 1:
                try {
                    // sleep between 1 and 10 seconds
                    Thread.sleep((long) (Math.random() * 9000) + 1000);
                } catch (InterruptedException e) {
                    // do nothing
                }
                break;
            case 2:
                try {
                    // sleep between 10 and 20 seconds
                    Thread.sleep((long) (Math.random() * 10000) + 10000);
                } catch (InterruptedException e) {
                    // do nothing
                }
                break;
            case 3:
                try {
                    // sleep between 20 and 30 seconds
                    Thread.sleep((long) (Math.random() * 10000) + 20000);
                } catch (InterruptedException e) {
                    // do nothing
                }
                break;
        }
    }


    /**
     * Sets the delay form for M3. M3 has 4 different delay forms:
     * 0: No delay - no delay will be applied.
     * 1: Small delay - a delay of between 1 and 10 seconds.
     * 2: Medium delay - a delay of between 10 and 20 seconds.
     * 3: Large delay - a delay of between 20 and 30 seconds.
     *
     * @param delayForm : int : the delay form to set for M3.
     */
    @Override
    public void setDelayForm(int delayForm) {
        if (delayForm < 0 || delayForm > 3) {
            throw new IllegalArgumentException("Invalid delay form: " + delayForm);
        }
        this.delayForm = delayForm;
    }


    /**
     * M3 will roll a die to determine if they should delay their response time. If the dice roll is 1, 2, or 3,
     * they will delay their response time.
     * If the dice roll is 4 or 5, they will not delay their response time.
     * If the dice roll is 6, they will go camping for a minute.
     *
     * @param connection : Socket : the connection to the member.
     */
    @Override
    public void rollDice(Socket connection) {
        int diceRoll = (int) (Math.random() * 6) + 1;
        switch (diceRoll) {
            case 1:
            case 2:
            case 3:
                delay();
                break;
            case 4:
            case 5:
                return;
            case 6:
                goCamping();
                break;
        }
    }


    /**
     * M3 will go camping for a minute, where they will be completely unresponsive for a minute.
     */
    private void goCamping() {
        try {
            Thread.sleep(60000); // M3 will go camping for a minute.
        } catch (InterruptedException e) {
            // do nothing
        }
    }
}
