package backend;

import backend.util.SimpleShadowMemory;
import tools.fasttrack_frontend.FTSerializedState;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FixedParallelBlockingQueueFrameAnalyzer {

    private final int workerThreadNum;

    private BlockingQueue<FTSerializedState>[] queues;

    private Thread[] workerThreads;

    private volatile boolean isNoMoreInput = false;

    private AtomicLong tackledAccess = new AtomicLong(0L);

    public FixedParallelBlockingQueueFrameAnalyzer(int workerThreadNum) {
        this.workerThreadNum = workerThreadNum;
        queues = new LinkedBlockingQueue[workerThreadNum];
        workerThreads = new Thread[workerThreadNum];
        for (int i = 0; i < workerThreadNum; i++) {
            queues[i] = new LinkedBlockingQueue<>();
            workerThreads[i] = new Thread(new WorkerThread(i, queues[i]));
            workerThreads[i].start();
        }
    }

    public void addFrame(FTSerializedState[] frame) {
        for (int i = 0; i < frame.length; i++) {
            FTSerializedState state = frame[i];
            int address = state.getAddress();
            // partition memory accesses in a round-robin manner
            int workerThreadTid = address % workerThreadNum;
            if (workerThreadTid < 0) {
                workerThreadTid = -workerThreadTid;
            }
            queues[workerThreadTid].offer(state);
        }
    }

    public void noMoreInput() {
        isNoMoreInput = true;
    }

    public void close() {
        try {
            for (Thread t : workerThreads) {
                t.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("tackled memory access: " + tackledAccess.get());
    }

    class WorkerThread implements Runnable {

        private int tid;

        private BlockingQueue<FTSerializedState> queue;

        private SimpleShadowMemory history = new SimpleShadowMemory();

        public WorkerThread(int tid, BlockingQueue<FTSerializedState> queue) {
            this.tid = tid;
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                if (isNoMoreInput && queue.isEmpty()) {
                    break;
                }
                try {
                    FTSerializedState state = queue.poll(1000, TimeUnit.MILLISECONDS);
                    if (state != null) {
                        history.add(state.getAddress(), state.isWrite(), state.getEvent(), state.getTicket(), state.getTid());
                        tackledAccess.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
