package testgrp;

import java.util.concurrent.ExecutionException;

import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;

import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.concurrent.Phaser;

/**
 * Increasing-Echo Quasar Example
 *
 * @author circlespainter
 */
public class ReproduceQuasar80 {
    private static final int THREADS = 3;
    private static final int FIBERS = 4;

    private static final int ATTEMPTS = 1000;

    private static final Phaser phaser = new Phaser(THREADS + FIBERS);

    private static Runnable toRunnable(final SuspendableRunnable sr) {
        return new Runnable() {
            @Override
            @Suspendable
            public void run() {
                try {
                    sr.run();
                } catch (SuspendExecution | InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
        };
    }

    public static void logStrand(final String msg) {
        System.out.println(Strand.currentStrand().getName() + ": " + msg);
    }

    public static void main(final String[] args) throws InterruptedException, ExecutionException {
        final TasksList tl = new TasksList();

        final SuspendableRunnable srTask = new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                logStrand("Running task: sleep 10");
                if (Strand.isCurrentFiber())
                    Fiber.sleep(1);
                logStrand("Ran task: sleep 10");
            }
        };

        final SuspendableRunnable sr = new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for (int i = 0 ; i < ATTEMPTS ; i++) {
                    logStrand("Phasing 1");
                    phaser.arriveAndAwaitAdvance();
                    logStrand("Phased 1, adding task");
                    tl.addTask(toRunnable(srTask));
                    logStrand("Phasing 2");
                    phaser.arriveAndAwaitAdvance();
                    logStrand("Phased 2, processing");
                    tl.processTasks();
                }
            }
        };

        final Runnable r = toRunnable(sr);

        final Strand[] strands = new Strand[THREADS + FIBERS];
        for(int i = 0 ; i < THREADS ; i++)
            strands[i] = Strand.of(new Thread(r));
        for(int i = THREADS ; i < THREADS + FIBERS ; i++)
            strands[i] = new Fiber(sr);
        for (Strand strand : strands) strand.start();
        for (Strand strand : strands) strand.join();
    }
}