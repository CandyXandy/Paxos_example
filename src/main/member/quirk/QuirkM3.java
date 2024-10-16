package member.quirk;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


/**
 * M3 is not as responsive as M1, but is more reliable than M2, and will generally be somewhat reliable, but
 * they may occasionally decide to go camping, where they will be completely unresponsive, and drop all
 * connections for a minute.
 */
public class QuirkM3 implements Quirk {

    private int delayForm;


    /**
     * M3 will delay their response time by up to 10 seconds, depending on the delay form.
     */
    private void delay() throws InterruptedException {
        switch (delayForm) {
            case 0:
                return;
            case 1:
                // sleep between 0 and 2 seconds
                TimeUnit.SECONDS.sleep(new Random().nextInt(0, 3));
                break;
            case 2:
                // sleep between 2 and 5 seconds
                TimeUnit.SECONDS.sleep(new Random().nextInt(2, 6));
                break;
            case 3:
                // sleep between 4 and 10 seconds
                TimeUnit.SECONDS.sleep(new Random().nextInt(4, 11));
                break;
        }
    }


    /**
     * Sets the delay form for M3. M3 has 4 different delay forms:
     * 0: No delay - no delay will be applied.
     * 1: Small delay - a delay of between 0 and 2 seconds
     * 2: Medium delay - a delay of between 2 and 5 seconds.
     * 3: Large delay - a delay of between 4 and 10 seconds.
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
     */
    @Override
    public void rollDice() throws InterruptedException {
        int diceRoll = (int) (Math.random() * 6) + 1;
        switch (diceRoll) {
            case 1:
            case 2:
            case 3:
                Logger.getLogger(QuirkM3.class.getName()).fine("M3 is delaying their response.");
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
     * M3 will go camping for 20 seconds, where they will be completely unresponsive.
     */
    private void goCamping() throws InterruptedException {
        Logger.getLogger(QuirkM3.class.getName()).info("M3 is going camping.");
        TimeUnit.SECONDS.sleep(20);
        Logger.getLogger(QuirkM3.class.getName()).info("M3 has returned from camping.");
    }
}
