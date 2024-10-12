package member;

/**
 * This enum represents the ports that the members of the Adelaide Suburbs Council
 * will use to communicate with each other.
 */
public enum Members {
    M1, // 4005
    M2, // 4006
    M3, // 4007
    M4, // 4008
    M5, // 4009
    M6, // 4010
    M7, // 4011
    M8, // 4012
    M9; // 4013



    /**
     * This method returns the port number of the member.
     *
     * @return : int : the port number of the member.
     */
    public int getPort() {
        return switch (this) {
            case M1 -> 4005;
            case M2 -> 4006;
            case M3 -> 4007;
            case M4 -> 4008;
            case M5 -> 4009;
            case M6 -> 4010;
            case M7 -> 4011;
            case M8 -> 4012;
            case M9 -> 4013;
        };
    }


    /**
     * This method returns the member number based on the member.
     * i.e. M1 will return 1.
     *
     * @param member : Members : the member to get the number of.
     * @return : int : the number of the member.
     */
    public static int getMemberNumber(Members member) {
        return switch (member) {
            case M1 -> 1;
            case M2 -> 2;
            case M3 -> 3;
            case M4 -> 4;
            case M5 -> 5;
            case M6 -> 6;
            case M7 -> 7;
            case M8 -> 8;
            case M9 -> 9;
        };
    }

    /**
     * This method returns the member based on the member number.
     *
     * @param memberNumber : int : the number of the member.
     * @return : Members : the member based on the member number. i.e. 1 will return M1.
     */
    public static Members getMember(int memberNumber) {
        return switch (memberNumber) {
            case 1 -> M1;
            case 2 -> M2;
            case 3 -> M3;
            case 4 -> M4;
            case 5 -> M5;
            case 6 -> M6;
            case 7 -> M7;
            case 8 -> M8;
            case 9 -> M9;
            default -> throw new IllegalArgumentException("Unexpected value: " + memberNumber);
        };
    }


    /**
     * This method returns the member based on the port number.
     *
     * @param port : int : the port number of the member.
     * @return : Members : the member based on the port number. i.e. 4005 will return M1.
     */
    public static Members getMemberFromPort(int port) {
        return switch (port) {
            case 4005 -> M1;
            case 4006 -> M2;
            case 4007 -> M3;
            case 4008 -> M4;
            case 4009 -> M5;
            case 4010 -> M6;
            case 4011 -> M7;
            case 4012 -> M8;
            case 4013 -> M9;
            default -> throw new IllegalArgumentException("Unexpected value: " + port);
        };
    }
}
