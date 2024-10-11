package member;


import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * This member class wants to be the council president, as such, it will ignore messages which could lead to
 * other members becoming president. It will only accept messages which will allow it to become president.
 * For example, it will promise to vote, but when it receives the value upon the accept request, it will reject
 * the value if M3 is not the value being proposed.
 * However, M3 will occasionally go camping and will not respond to any messages during this time. There is a
 * one in ten chance that M3 will go camping when any of its methods are invoked. M3 will remain camping for
 * 2 minutes.
 * M3 takes a little longer than M1 to respond to messages, but not as long as M2, as such, it will call a busy method
 * before any of its methods to simulate a 'busy' task. This busy task is a random sleep between 0 and 5 seconds.
 */
public class MemberM3 extends MemberImpl {



    public MemberM3(int memberNumber, boolean isProposer) {
        super(memberNumber, isProposer);
    }

    @Override
    public void prepare() {
        isItCampingTime();
        busy();
        System.out.println("MemberM3 is preparing proposal number ");
        super.prepare();
    }

    @Override
    public void promise(Members sender, int proposalNum, BufferedReader in, PrintWriter out) {
        isItCampingTime();
        busy();
        System.out.println("MemberM3 is promising proposal number " + proposalNum);
        super.promise(sender, proposalNum, in, out);
    }

    @Override
    public void acceptRequest(Members toVoteFor, int proposalNum) {
        isItCampingTime();
        busy();
        System.out.println("MemberM3 is accepting request for proposal number " + proposalNum);
        super.acceptRequest(toVoteFor, proposalNum);
    }

    @Override
    public void accept(int proposalNum) {
        isItCampingTime();
        busy();
        System.out.println("MemberM3 is accepting proposal number " + proposalNum);
        super.accept(proposalNum);
    }

    @Override
    public void reject(int proposalNum) {
        isItCampingTime();
        busy();
        System.out.println("MemberM3 is rejecting proposal number " + proposalNum);
        super.reject(proposalNum);
    }

    @Override
    public void decide(int proposalNum) {
        isItCampingTime();
        busy();
        System.out.println("MemberM3 is deciding on proposal number " + proposalNum);
        super.decide(proposalNum);
    }


    /**
     * This method allows M3 to go camping, where it will remain for 2 minutes, ignoring all messages.
     */
    public void goCamping() {
        System.out.println("MemberM3 is going camping");
        try {
            TimeUnit.MINUTES.sleep(2);
        } catch (InterruptedException e) {
            // I have been forcibly returned from my camping trip :(
            if (Thread.interrupted()) {
                System.err.println("MemberM3 has been forcibly returned from camping");
                Thread.currentThread().interrupt(); // restore interrupted status
            }
        } catch (Exception e) {
            // if any other exception occurs, ignore it, we are camping
        }
    }


    /**
     * Provides a one in ten chance of invoking the goCamping method.
     */
    public void isItCampingTime() {
        // one in ten chance of going camping
        if (new Random().nextInt(10) == 0) {
            goCamping();
        }
    }



    /**
     * This method simulates the member being busy with other tasks. It will sleep for a random amount of time
     * between 0 and 5 seconds before returning.
     */
    private void busy() {
        Random random = new Random();
        int randomInt = random.nextInt(5);
        try {
            TimeUnit.SECONDS.sleep(randomInt); // sleep for a random amount of time between 0 and 5 seconds
        } catch (InterruptedException e) {
            // why did you wake me up from my slumber?
            if (Thread.interrupted()) {
                System.err.println("Member " + getMemberNumber() + " was interrupted while busy");
                Thread.currentThread().interrupt(); // restore the interrupted status
            }
        }
    }

}
