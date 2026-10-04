import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe bounded queue using ReentrantLock + Condition.
 * Producers block when full; consumers block when empty.
 * A "finished" flag signals workers that no more tasks are coming.
 */
public class TaskQueue {
    private final Queue<Task> queue = new LinkedList<>();
    private final int maxSize;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private boolean finished = false;

    public TaskQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    /** Adds a task, blocking if the queue is full. */
    public void addTask(Task task) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == maxSize) {
                notFull.await();
            }
            queue.add(task);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves a task, blocking if empty. Returns null when the
     * queue has been marked finished and is empty (signals workers to exit).
     */
    public Task getTask() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty() && !finished) {
                notEmpty.await();
            }
            if (queue.isEmpty()) {
                return null;
            }
            Task task = queue.poll();
            notFull.signal();
            return task;
        } finally {
            lock.unlock();
        }
    }

    /** Marks the queue finished and wakes all waiting consumers. */
    public void markFinished() {
        lock.lock();
        try {
            finished = true;
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
