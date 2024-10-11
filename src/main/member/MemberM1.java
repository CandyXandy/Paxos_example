package member;

import java.io.BufferedReader;
import java.io.PrintWriter;

/**
 * This member class wants to be the council president, as such, it will ignore messages which could lead to
 * other members becoming president. It will only accept messages which will allow it to become president.
 * For example, it will promise to vote, but when it receives the value upon the accept request, it will reject
 * the value if M1 is not the value being proposed.
 * M1 is the fastest to respond to messages, as M1 is very chatty over social media and answers their phone
 * almost instantly, almost as if they have an in-bran connection to their phone. As such, M1 will call a busy method
 * with a random sleep between 0 and 2 seconds before any of its methods to simulate a 'busy' task.
 */
public class MemberM1 extends MemberImpl {


    public MemberM1(int memberNumber, boolean isProposer) {
        super(memberNumber, isProposer);
    }

    @Override
    public void prepare() {
        System.out.println("MemberM1 is preparing proposal number ");
        busy();
        super.prepare();
    }

    @Override
    public void promise(Members sender, int proposalNum, BufferedReader in, PrintWriter out) {
        System.out.println("MemberM1 is promising proposal number " + proposalNum);
        busy();
        super.promise(sender, proposalNum, in, out);
    }

    @Override
    public void acceptRequest(Members toVoteFor, int proposalNum) {
        System.out.println("MemberM1 is accepting request for proposal number " + proposalNum);
        busy();
        super.acceptRequest(toVoteFor, proposalNum);
    }

    @Override
    public void accept(int proposalNum) {
        System.out.println("MemberM1 is accepting proposal number " + proposalNum);
        busy();
        super.accept(proposalNum);
    }

    @Override
    public void reject(int proposalNum) {
        System.out.println("MemberM1 is rejecting proposal number " + proposalNum);
        busy();
        super.reject(proposalNum);
    }

    @Override
    public void decide(int proposalNum) {
        System.out.println("MemberM1 is deciding on proposal number " + proposalNum);
        busy();
        super.decide(proposalNum);
    }


    /**
     * This method simulates the member being busy with other tasks. It will sleep for a random amount of time
     * between 0 and 2 seconds before returning.
     */
    private void busy() {
        try {
            Thread.sleep((long) (Math.random() * 2000));
        } catch (InterruptedException e) {
            if (Thread.interrupted()) {
                System.out.println("MemberM1 was interrupted while busy");
                Thread.currentThread().interrupt();
            }
        }
    }
}
