package honeyroasted.occurrence;

public interface EventPriority {

    int FIRST_POSSIBLE = Integer.MIN_VALUE;
    int EARLIER = -2;
    int EARLY = -1;
    int NEUTRAL = 0;
    int LATE = 1;
    int LATER = 2;
    int LAST_POSSIBLE = Integer.MAX_VALUE;

}
