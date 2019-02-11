package org.aion.type.api.timer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Runnable for VMTimer, allowing us to leverage CachedThreadPools for fast thread allocation
 *
 * @author yao
 */
public class StackTimerRunnable implements Runnable {
    private final Deque<TimerTask> stack = new ArrayDeque<>();
    private final BlockingQueue<TimerTask> inputQueue = new LinkedBlockingQueue<>();

    private volatile boolean done;

    public StackTimerRunnable() {}

    public void submit(TimerTask task) {
        synchronized (inputQueue) {
            this.inputQueue.add(task);
        }
    }

    @Override
    public void run() {
        try {
            loop();
        } finally {
            this.stack.clear();
        }
        this.done = true;
    }

    public void shutdown() {
        inputQueue.add(new PoisonPillTask());
    }

    private void loop() {
        MAIN_LOOP:
        while (!Thread.currentThread().isInterrupted()) {
            List<TimerTask> tasks = new ArrayList<>();
            if (stack.isEmpty() && inputQueue.isEmpty()) {
                try {
                    // works under the assumption that task should never be null
                    // please do not feed it nulls
                    tasks.add(inputQueue.take());
                } catch (InterruptedException e) {
                    break MAIN_LOOP; // if any interrupted exceptions, shut down
                    // the timer
                }
            }

            /**
             * The stack might not be empty, in this case, we need to check for any new items that
             * may have gathered inside our queue
             */
            synchronized (inputQueue) {
                if (!inputQueue.isEmpty()) {
                    inputQueue.drainTo(tasks);
                }
            }

            // at this point we should have all tasks, check if any of them are
            // poison pills
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i) instanceof PoisonPillTask) {
                    break MAIN_LOOP;
                }
            }

            /**
             * At this point, stack may be empty, but we should always have a task we perform the
             * following operations at this point:
             *
             * <p>1) If stack is not empty, check for done() at the top of the stack, and iterate
             * down the stack until we reach a task that is not done
             *
             * <p>2) We check for timeouts on any task that is not done, working under the
             * assumption of a stack model, tasks that are not yet done should not have any tasks
             * that are done below them in the stack
             *
             * <p>3) Remove any tasks (iteratively) that are timed out
             *
             * <p>4) Insert any new tasks for the queue
             */
            if (!stack.isEmpty()) {
                // check tasks that are done first
                {
                    TimerTask peeked = stack.peek();
                    while (peeked != null && peeked.getDone()) {
                        stack.pop();
                        peeked = stack.peek();
                    }
                }

                // check tasks that are timed out
                {
                    TimerTask peeked = stack.peek();
                    long currentTime = System.nanoTime();
                    while (peeked != null && (currentTime > peeked.getEndTime())) {
                        peeked.setTimeOut();
                        stack.pop();
                        peeked = stack.peek();
                    }
                }
            }

            /** Finally add the new tasks */
            for (int i = 0; i < tasks.size(); i++) {
                stack.push(tasks.get(i));
            }
        }
    }

    /**
     * Checks if this runnable is done
     *
     * @return
     */
    public boolean done() {
        return this.done;
    }
}
