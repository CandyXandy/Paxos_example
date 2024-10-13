package member.quirk;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


/**
 * M2 lives in the Adelaide Hills, and so has a very poor internet connection. Their responses are unreliable,
 * and they often drop connections, and take a long time to respond. However, M2 will occasionally visit
 * Sheoak Cafe to work, where they respond instantly and never drop connections.
 */
public class QuirkM2 implements Quirk {

    private boolean atCafe = false;
    private int delayForm;

    /**
     * M2 will delay their response time by up to 15 seconds, depending on the delay form.
     */
    private void delay() throws InterruptedException {
        if (atCafe) {
            return;
        }
        switch (delayForm) {
            case 0:
                return;
            case 1:
                    // sleep between 1 and 5 seconds
                    TimeUnit.SECONDS.sleep(new Random().nextInt(1, 6));
                break;
            case 2:
                    // sleep between 5 and 10 seconds
                    TimeUnit.SECONDS.sleep(new Random().nextInt(5, 11));
                break;
            case 3:
                    // sleep between 10 and 15 seconds
                    TimeUnit.SECONDS.sleep(new Random().nextInt(10, 16));
                break;
        }
    }

    /**
     * Sets the delay form for M2. M2 has 4 different delay forms:
     * 0: No delay - no delay will be applied.
     * 1: Small delay - a delay of between 0 and 5 seconds.
     * 2: Large delay - a delay of between 5 and 10 seconds.
     * 3: Unresponsive - a delay of between 10 and 15 seconds.
     *
     * @param delayForm : int : the delay form to set for M2.
     */
    @Override
    public void setDelayForm(int delayForm) {
        if (delayForm < 0 || delayForm > 3) {
            throw new IllegalArgumentException("Delay form must be between 0 and 3.");
        }
        this.delayForm = delayForm;
    }


    /**
     * M2 has a 1/3 chance of doing nothing, a 1/2 chance of delaying a response, and a 1/6 chance
     * of visiting the cafe, where they will be reliable for a minute.
     */
    @Override
    public void rollDice() throws InterruptedException {
        int diceRoll = new Random().nextInt(6) + 1;
        switch (diceRoll) {
            case 1:
            case 2:
            case 3:
                Logger.getLogger(QuirkM2.class.getName()).fine("M2 is delaying their response.");
                delay();
                break;
            case 4:
            case 5:
                break;
            case 6:
                if (!atCafe) {
                    visitCafe(); // can't visit the cafe if we're already there!
                }
                break;
        }
    }


    /**
     * Visits Sheoak Cafe for a minute. During this time, M2 will have instant response times, and will never
     * drop connections.
     * After 2 minutes, a scheduled task will set the value back to false, signifying M2 has left the cafe.
     */
    private void visitCafe() {
        Logger.getLogger(QuirkM2.class.getName()).info("M2 is visiting Sheoak Cafe.");
        atCafe = true;
        // for the next minute M2 will be reliable.
        ScheduledExecutorService n = Executors.newSingleThreadScheduledExecutor();
        // set the value back to false after a minute.
        n.schedule(() ->  {
            atCafe = false;
            Logger.getAnonymousLogger().info("M2 has left Sheoak Cafe.");
            }, 60, TimeUnit.SECONDS);
    }



}
