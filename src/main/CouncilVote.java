import member.*;

import java.util.logging.Logger;

public class CouncilVote {

    private final static Logger logger = Logger.getLogger(CouncilVote.class.getName());

    /**
     * Main method.
     * Decides which member of the voting council this process is and then decides if it is a proposer
     * or an acceptor and starts the corresponding process.
     *
     * @param args the command line arguments
     *             args[0] is the member number (1-9)
     *             args[1] determines if this process is a proposer or an acceptor.
     *             args[2] is if this process is to run in test mode, that is, without each process having quirks.
     *             Any value passed in args[2] will be considered true.
     *             quirks: quirks are where the process will behave according to a set of pre-defined quirks.
     *             For example, a process may be incredibly responsive, unreliable, or slow. This is to allow
     *             for testing of scenarios such as processes acting according to their quirks, or acting as normal.
     */
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: java CouncilVote <member number> <-a|-p> [quirks]");
            System.exit(1);
        }
        int memberNumber = readMemberNumberArg(args[0]);
        boolean isProposer = readProposerArg(args[1]);
        boolean isTestMode = args.length == 3;

        if (isTestMode) {
            logger.info("Running in test mode.");
            logger.info("Creating Member " + memberNumber + " as " + (isProposer ? "Proposer" : "Acceptor"));

            Member member = new MemberImpl(memberNumber, isProposer, true);
            member.run();
        } else {
            createAndRunMember(memberNumber, isProposer);
        }
    }

    /**
     * Reads the member number argument and returns the member number.
     * Exits the program if the argument is not an integer or is not between 1 and 9.
     *
     * @param arg : String : the argument to read.
     * @return int : the member number.
     */
    private static int readMemberNumberArg(String arg) {
        int memberNumber = 0;
        try {
            memberNumber = Integer.parseInt(arg);
            if (memberNumber < 1 || memberNumber > 9) {
                System.out.println("Member number must be between 1 and 9.");
                System.exit(1);
            }
        } catch (NumberFormatException e) {
            System.out.println("Member number must be an integer.");
            System.exit(1);
        }
        return memberNumber;
    }


    /**
     * Reads the proposer argument and returns whether the member is a proposer or not.
     *
     * @param arg : String : the argument to read.
     * @return boolean : whether the member is a proposer or not.
     */
    private static boolean readProposerArg(String arg) {
        if (arg.equalsIgnoreCase("-p")) {
            return true;
        } else if (!arg.equalsIgnoreCase("-a")) {
            System.out.println("Is proposer must be either '-a' or '-p'.");
            System.exit(1);
        }
        return false;
    }


    /**
     * Creates and runs a member of the council.
     * Determines which kind of member to run based on the member number.
     *
     * @param memberNumber : int : the number of the member in the council.
     * @param isProposer : boolean : whether the member is a proposer or not.
     */
    private static void createAndRunMember(int memberNumber, boolean isProposer) {
        logger.info("Creating Member " + memberNumber + " as " + (isProposer ? "Proposer" : "Acceptor"));
        Member member = new MemberImpl(memberNumber, isProposer, false);
        member.run();
    }
}
