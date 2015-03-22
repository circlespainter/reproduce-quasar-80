package testgrp;

import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import javolution.util.FastTable;

import javax.annotation.Nonnull;

public class TasksList {
    /**
     * Lock for providing effective atomicity for reading updating the tsk list
     */
    private final ReentrantLock LOCK = new ReentrantLock();

    /**
     * A lists of tasks to be done on the this thread only.
     */
    private final FastTable<Runnable> tasks = new FastTable<>();

    @Suspendable
    public void processTasks() {

        LOCK.lock();
        try {
            final int size = tasks.size();
            for (int i = size - 1; i >= 0; --i) {

                @Nonnull
                final Runnable t = tasks.get(i);

                t.run();
            }

            tasks.clear();
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Tasks can be added by any thread
     */
    @Suspendable
    public void addTask(@Nonnull Runnable t) {
        LOCK.lock();
        try {
            tasks.addLast(t);
        } finally {
            LOCK.unlock();
        }
    }
}