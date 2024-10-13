package member.quirk;

public class QuirkM1 implements Quirk {

    private int delayForm;

    /**
     * M1 will delay their response time by up to 10 seconds, depending on the delay form.
     */
    private void delay() {
        // M1 will never delay their response time.
    }

    /**
     * Sets the delay form for M1. M1 will never delay their response time.
     * 0: No delay - no delay will be applied.
     * 1: No delay - no delay will be applied.
     * 2: No delay - no delay will be applied.
     * 3: No delay - no delay will be applied.
     *
     * @param delayForm : int : the delay form to set for M1.
     */
    @Override
    public void setDelayForm(int delayForm) {
        this.delayForm = delayForm;
    }

    @Override
    public void rollDice() throws InterruptedException {
        // M1 will never delay their response time.
    }
}
