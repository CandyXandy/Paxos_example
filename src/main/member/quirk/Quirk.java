package member.quirk;

/**
 * This interface defines the base methods that any member of the Adelaide Suburbs Council may invoke
 * when they are acting according to their 'quirks'. A quirk is a unique behavior that a member may
 * exhibit, such as being incredibly responsive, unreliable, or slow. This interface is implemented
 * by the various members in the form of quirk<number> classes, where <number> is the member number.
 * For example, quirkM1, quirkM2, quirkM3, etc.
 */
public interface Quirk {
    void setDelayForm(int delayForm); // switches between no, small, large, and unresponsive delay.

    void rollDice() throws InterruptedException; // Rolls a die to determine the member's behavior.
}
