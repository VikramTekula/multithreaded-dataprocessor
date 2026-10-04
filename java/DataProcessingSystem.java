import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point. Sets up the queue, starts the producer thread,
 * spins up worker threads via an ExecutorService, waits for completion,
 * and cleans up resources in a finally block.
 */
public class DataProcessingSystem {
    private static final int NUM_WORKERS = 4;
    private static final int NUM_TASKS = 20;
    private static final int QUEUE_CAPACITY = 10;
    private static final String OUTPUT_FILE = "results_java.txt";

    public static void main(String[] args) {
        System.out.println("=== Multi-threaded Data Processing System (Java) ===");

        TaskQueue queue = new TaskQueue(QUEUE_CAPACITY);
        ResultWriter resultWriter = null;
        ExecutorService executor = null;

        try {
            resultWriter = new ResultWriter(OUTPUT_FILE);
            final ResultWriter rw = resultWriter;

            // ---- Producer thread ----
            Thread producer = new Thread(() -> {
                try {
                    for (int i = 1; i <= NUM_TASKS; i++) {
                        queue.addTask(new Task(i, "ride_request_" + i));
                    }
                    queue.markFinished();
                    System.out.println("[Producer] All " + NUM_TASKS + " tasks added.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("[Producer] Interrupted: " + e.getMessage());
                }
            }, "Producer");
            producer.start();

            // ---- Worker pool ----
            executor = Executors.newFixedThreadPool(NUM_WORKERS);
            for (int i = 1; i <= NUM_WORKERS; i++) {
                executor.submit(new Worker(i, queue, rw));
            }

            // ---- Graceful shutdown ----
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("Timeout: forcing shutdown.");
                executor.shutdownNow();
            }
            producer.join();

            System.out.println("=== All workers completed. Results in "
                    + OUTPUT_FILE + " ===");

        } catch (IOException e) {
            System.err.println("File I/O error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread interrupted: " + e.getMessage());
        } finally {
            if (resultWriter != null) {
                try { resultWriter.close(); }
                catch (IOException e) {
                    System.err.println("Failed to close result file: " + e.getMessage());
                }
            }
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
            System.out.println("System shutdown complete.");
        }
    }
}
