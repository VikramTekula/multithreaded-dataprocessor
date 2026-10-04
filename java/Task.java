/**
 * Represents a single unit of work (a ride request) to be processed.
 * Immutable so it can be safely shared between threads.
 */
public class Task {
    private final int id;
    private final String payload;

    public Task(int id, String payload) {
        this.id = id;
        this.payload = payload;
    }

    public int getId() { return id; }
    public String getPayload() { return payload; }

    @Override
    public String toString() {
        return "Task{id=" + id + ", payload='" + payload + "'}";
    }
}
