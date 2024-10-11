package member;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * This member class is the default member of the Adelaide Suburbs Council. It is the most common type of member
 * and behaves without any particular preferences or animosities. It will try to vote fairly. It's job keeps
 * it busy and as such it's response times will vary.
 */
public class MemberDefault extends MemberImpl {


    public MemberDefault(int memberNumber, boolean isProposer) {
        super(memberNumber, isProposer);
    }

    @Override
    public void prepare() {
        System.out.println("Member " + getMemberNumber() + " is preparing proposal number ");
        busy();
        super.prepare();
    }

    @Override
    public void promise(Members sender, int proposalNum, BufferedReader in, PrintWriter out) {
        System.out.println("Member " + getMemberNumber() + " is promising proposal number " + proposalNum);
        busy();
        super.promise(sender, proposalNum, in, out);
    }

    @Override
    public void acceptRequest(Members toVoteFor, int proposalNum) {
        System.out.println("Member " + getMemberNumber() + " is accepting request for proposal number " + proposalNum);
        busy();
        super.acceptRequest(toVoteFor, proposalNum);
    }

    @Override
    public void accept(int proposalNum) {
        System.out.println("Member " + getMemberNumber() + " is accepting proposal number " + proposalNum);
        busy();
        super.accept(proposalNum);
    }

    @Override
    public void reject(int proposalNum) {
        System.out.println("Member " + getMemberNumber() + " is rejecting proposal number " + proposalNum);
        busy();
        super.reject(proposalNum);
    }

    @Override
    public void decide(int proposalNum) {
        System.out.println("Member " + getMemberNumber() + " is deciding on proposal number " + proposalNum);
        busy();
        super.decide(proposalNum);
    }


    /**
     * This method simulates the member being busy with other tasks. It will sleep for a random amount of time
     * between 0 and 10 seconds before returning.
     */
    public void busy() {
        Random random = new Random();
        int randomInt = random.nextInt(10);
        try {
            TimeUnit.SECONDS.sleep(randomInt); // sleep for a random amount of time between 0 and 20 seconds
        } catch (InterruptedException e) {
            // why did you wake me up from my slumber?
            if (Thread.interrupted()) {
                System.err.println("Member " + getMemberNumber() + " was interrupted while busy");
                Thread.currentThread().interrupt(); // restore the interrupted status
            }
        }
    }
}
