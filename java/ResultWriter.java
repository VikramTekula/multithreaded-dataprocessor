import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Thread-safe writer that serializes concurrent writes to an output file.
 * Uses a synchronized monitor so only one worker writes at a time.
 */
public class ResultWriter {
    private final BufferedWriter writer;
    private final Object lock = new Object();

    public ResultWriter(String filename) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(filename, false));
    }

    public void writeResult(String result) throws IOException {
        synchronized (lock) {
            writer.write(result);
            writer.newLine();
            writer.flush();
        }
    }

    public void close() throws IOException {
        synchronized (lock) {
            writer.close();
        }
    }
}
