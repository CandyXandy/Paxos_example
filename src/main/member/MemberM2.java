package member;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

/**
 * This member class wants to be the council president, as such, it will ignore messages which could lead to
 * other members becoming president. It will only accept messages which will allow it to become president.
 * For example, it will promise to vote, but when it receives the value upon the accept request, it will reject
 * the value if M2 is not the value being proposed. M2 also lives in the Adelaide Hills and has a very poor
 * internet connection, and will take a long time to respond to messages, if at all. There is a one in 3 chance that
 * M2 will just ignore a message when any of its methods are invoked. Sometimes, however, M2 will go work at
 * Sheoak Cafe and will start responding instantly without fail for the next minute. There is a one in 10 chance that
 * M2 will be at Sheoak Cafe when any of its methods are invoked.
 * M2 takes the longest to respond to messages, and will call a busy method before any of its methods to simulate a
 * 'busy' task. This busy task is a random sleep between 0 and 30 seconds.
 */
public class MemberM2 extends MemberImpl {

    private final AtomicBoolean isAtSheoakCafe;

    public MemberM2(int memberNumber, boolean isProposer) {
        super(memberNumber, isProposer);
        this.isAtSheoakCafe = new AtomicBoolean(false);
    }


    @Override
    public void prepare() {
        if (didMemberDisconnect()) {
            return;
        }
        if (!isAtSheoakCafe.get()) {
            isItCafeTime();
            busy();
        }
        System.out.println("Member M2 is preparing proposal number ");
        super.prepare();
    }

    @Override
    public void promise(Members sender, int proposalNum, BufferedReader in, PrintWriter out) {
        if (didMemberDisconnect()) {
            return;
        }
        if (!isAtSheoakCafe.get()) {
            isItCafeTime();
            busy();
        }
        System.out.println("Member M2 is promising proposal number " + proposalNum);
        super.promise(sender, proposalNum, in, out);
    }

    @Override
    public void acceptRequest(Members toVoteFor, int proposalNum) {
        if (didMemberDisconnect()) {
            return;
        }
        if (!isAtSheoakCafe.get()) {
            isItCafeTime();
            busy();
        }
        System.out.println("Member M2 is accepting request for proposal number " + proposalNum);
        super.acceptRequest(toVoteFor, proposalNum);
    }


    @Override
    public void accept(int proposalNum) {
        if (didMemberDisconnect()) {
            return;
        }
        if (!isAtSheoakCafe.get()) {
            isItCafeTime();
            busy();
        }
        System.out.println("Member M2 is accepting proposal number " + proposalNum);
        super.accept(proposalNum);
    }

    @Override
    public void reject(int proposalNum) {
        if (didMemberDisconnect()) {
            return;
        }
        if (!isAtSheoakCafe.get()) {
            isItCafeTime();
            busy();
        }
        System.out.println("Member M2 is rejecting proposal number " + proposalNum);
        super.reject(proposalNum);
    }

    @Override
    public void decide(int proposalNum) {
        if (didMemberDisconnect()) {
            return;
        }
        if (!isAtSheoakCafe.get()) {
            isItCafeTime();
            busy();
        }
        System.out.println("Member M2 is deciding on proposal number " + proposalNum);
        super.decide(proposalNum);
    }


    /**
     * Has a 1 in 3 chance of returning true.
     *
     * @return true if the random number is less than 0.33, false otherwise.
     */
    private boolean didMemberDisconnect() {
        return Math.random() < 0.33;
    }


    /**
     * Simulates the member being busy with other tasks. It will sleep for a random amount of time between 0 and 30
     * seconds before returning.
     */
    private void busy() {
        try {
            Thread.sleep((long) (Math.random() * 30000));
        } catch (InterruptedException e) {
            if (Thread.interrupted()) {
                System.out.println("Member M2 was interrupted while busy");
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Has a 1 in 10 chance of calling the goToCafe method.
     */
    private void isItCafeTime() {
        if (Math.random() < 0.1) {
            goToCafe();
        }
    }


    /**
     * Simulates the member going to work at Sheoak Cafe. The member will respond instantly without fail for the next
     * minute. A scheduled executor is used to set the member back to not being at Sheoak Cafe after a minute.
     */
    private void goToCafe() {
        isAtSheoakCafe.set(true);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> isAtSheoakCafe.set(false), 1, TimeUnit.MINUTES);
    }
}
