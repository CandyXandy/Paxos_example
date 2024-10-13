package member.quirk;

import java.io.IOException;
import java.net.Socket;
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
     * M2 will delay their response time by up to 60 seconds, depending on the delay form.
     */
    private void delay() {
        if (atCafe) {
            return;
        }
        switch (delayForm) {
            case 0:
                return;
            case 1:
                try {
                    // sleep between 1 and 10 seconds
                    TimeUnit.SECONDS.sleep(new Random().nextInt(1, 11));
                } catch (InterruptedException e) {
                    // do nothing
                }
                break;
            case 2:
                try {
                    // sleep between 10 and 30 seconds
                    TimeUnit.SECONDS.sleep(new Random().nextInt(10, 31));
                } catch (InterruptedException e) {
                    // do nothing
                }
                break;
            case 3:
                try {
                    TimeUnit.SECONDS.sleep(new Random().nextInt(30, 61));
                } catch (InterruptedException e) {
                    // do nothing
                }
                break;
        }
    }

    /**
     * Sets the delay form for M2. M2 has 4 different delay forms:
     * 0: No delay - no delay will be applied.
     * 1: Small delay - a delay of between 1 and 10 seconds.
     * 2: Large delay - a delay of between 10 and 30 seconds.
     * 3: Unresponsive - a delay of between 30 and 60 seconds.
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
     * M2 drops the connection, closing the socket.
     *
     * @param socket : Socket : the socket to drop the connection for.
     * @throws IOException : if the socket cannot be closed.
     */
    private void dropConnection(Socket socket) throws IOException {
        if (atCafe) {
            return;
        }
        socket.close(); // kill the socket, dropping the connection.
    }

    /**
     * M2 has a 1/3 chance of dropping a connection, a 1/3 chance of delaying a response, and a 1/6 chance
     * of visiting the cafe, where they will be reliable for a minute.
     *
     * @param connection : Socket : the connection to drop.
     */
    @Override
    public void rollDice(Socket connection) throws IOException {
        int diceRoll = new Random().nextInt(6) + 1;
        switch (diceRoll) {
            case 1:
            case 2:
                Logger.getLogger(QuirkM2.class.getName()).fine("M2 is delaying their response.");
                delay();
                break;
            case 3:
            case 4:
            case 5:
                Logger.getLogger(QuirkM2.class.getName()).fine("M2 is dropping the connection.");
                dropConnection(connection);
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
     * After a minute, a scheduled task will set the value back to false, signifying M2 has left the cafe.
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
