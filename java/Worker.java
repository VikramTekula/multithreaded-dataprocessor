import java.io.IOException;

/**
 * Worker thread: repeatedly pulls tasks from the queue, processes them
 * (simulated with a delay), and writes results to the shared writer.
 * Includes try-catch for InterruptedException, IOException, and any
 * unexpected runtime exception.
 */
public class Worker implements Runnable {
    private final int workerId;
    private final TaskQueue queue;
    private final ResultWriter resultWriter;

    public Worker(int workerId, TaskQueue queue, ResultWriter resultWriter) {
        this.workerId = workerId;
        this.queue = queue;
        this.resultWriter = resultWriter;
    }

    @Override
    public void run() {
        System.out.println("[Worker-" + workerId + "] STARTED");
        try {
            while (true) {
                Task task = queue.getTask();
                if (task == null) {
                    break; // no more work
                }
                String result = processTask(task);
                resultWriter.writeResult(result);
                System.out.println("[Worker-" + workerId + "] processed " + task);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Worker-" + workerId + "] INTERRUPTED: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("[Worker-" + workerId + "] IO ERROR: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Worker-" + workerId + "] ERROR: " + e.getMessage());
        } finally {
            System.out.println("[Worker-" + workerId + "] COMPLETED");
        }
    }

    /** Simulates expensive work using Thread.sleep and transforms the payload. */
    private String processTask(Task task) throws InterruptedException {
        Thread.sleep(200 + (int) (Math.random() * 300));
        return "Worker-" + workerId
                + " | Task-" + task.getId()
                + " | Processed: " + task.getPayload().toUpperCase();
    }
}
