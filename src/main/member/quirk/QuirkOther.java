package member.quirk;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * This class represents a member of the Adelaide Suburbs Council who is not M1, M2, or M3.
 * This member's job keeps them fairly busy and so their response time will vary, but they are
 * otherwise reliable.
 */
public class QuirkOther implements Quirk {

    private int delayForm;

    /**
     * Delays the member's response according to their delay form.
     * This is achieved by sleeping the thread for a random amount of time.
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
                // sleep between 1 and 5 seconds
                TimeUnit.SECONDS.sleep(new Random().nextInt(1, 6));

                break;
            case 3:
                // sleep between 3 and 10 seconds
                TimeUnit.SECONDS.sleep(new Random().nextInt(3, 11));

                break;
        }
    }


    /**
     * Sets the delay form for the member. The member has 4 different delay forms:
     * 0: No delay - no delay will be applied.
     * 1: Small delay - a delay of between 0 and 2 seconds.
     * 2: Medium delay - a delay of between 1 and 5 seconds.
     * 3: Large delay - a delay of between 3 and 10 seconds.
     *
     * @param delayForm : int : the delay form to set for the member.
     */
    @Override
    public void setDelayForm(int delayForm) {
        if (delayForm < 0 || delayForm > 3) {
            throw new IllegalArgumentException("Invalid delay form: " + delayForm);
        }
        this.delayForm = delayForm;
    }

    /**
     * The member rolls a die to determine if they should delay their response time.
     * If the dice roll is 1, 2, or 3, they will delay their response time.
     */
    @Override
    public void rollDice() throws InterruptedException {
        int dieRoll = (int) (Math.random() * 6) + 1;
        if (dieRoll == 1 || dieRoll == 2 || dieRoll == 3) {
            delay();
        }
    }
}
