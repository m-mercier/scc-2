package simulation.jobs.navigation;

public class Route {

    private static final int START_STATION =  5;

    private int[] sequence;
    private int current;

    public Route(int[] sequence) {
        this.sequence = sequence;
        this.current = START_STATION;
    }

    public int next() {
        current = (current == START_STATION) ? 0 : current+1;
        return (current < sequence.length) ? sequence[current] : START_STATION;
    }

    public int current() {
        if (current != START_STATION) {
            return (current < sequence.length) ? sequence[current] : START_STATION;
        } else {
            return START_STATION;
        }
    }

    public String toString() {
        String route = "[";
        for (int i = 0; i < sequence.length; i++) {
            route += String.format("%d ", sequence[i]);
        }
        route += "]";
        return route;
    }
}
