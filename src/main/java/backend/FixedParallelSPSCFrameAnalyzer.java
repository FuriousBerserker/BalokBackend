package backend;

import backend.util.SimpleShadowMemory;
import org.jctools.queues.SpscLinkedQueue;
import tools.fasttrack_frontend.FTSerializedState;

import java.util.concurrent.atomic.AtomicLong;

public class FixedParallelSPSCFrameAnalyzer {

    private final int workerThreadNum;

    private SpscLinkedQueue<FTSerializedState>[] queues;

    private Thread[] workerThreads;

    private volatile boolean isNoMoreInput = false;

    private AtomicLong tackledAccess = new AtomicLong(0L);

    private static int MAX_POLL_TIME = 10;

    private static int MAX_QUEUE_SIZE = 1000;

    private Status[] status;

    public FixedParallelSPSCFrameAnalyzer(int workerThreadNum) {
        this.workerThreadNum = workerThreadNum;
        queues = new SpscLinkedQueue[workerThreadNum];
        workerThreads = new Thread[workerThreadNum];
        status = new Status[workerThreadNum];
        for (int i = 0; i < workerThreadNum; i++) {
            queues[i] = new SpscLinkedQueue<>();
            status[i] = new Status();
            workerThreads[i] = new Thread(new WorkerThread(i, queues[i], status[i]));
            workerThreads[i].start();
        }
    }

    public void addFrame(FTSerializedState[] frame) {
        for (int i = 0; i < frame.length; i++) {
            FTSerializedState state = frame[i];
            int address = frame[i].getAddress();
            // partition memory accesses in a round-robin manner
            int workerThreadTid = address % workerThreadNum;
            if (workerThreadTid < 0) {
                workerThreadTid = -workerThreadTid;
            }
            queues[workerThreadTid].offer(state);
            status[workerThreadTid].submit();
        }

        for (int i = 0; i < status.length; i++) {
            if (status[i].untackled() >= MAX_QUEUE_SIZE) {
                synchronized (status[i]) {
                    try {
                        //System.out.println("input thread wait on thread " + i);
                        status[i].wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //System.out.println("input thread wake up");
            }
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

    public AtomicLong getTackledAccess() {
        return tackledAccess;
    }

    class WorkerThread implements Runnable {

        private int tid;

        private SpscLinkedQueue<FTSerializedState> queue;

        private Status status;

        private SimpleShadowMemory history = new SimpleShadowMemory();

        private int tackledAccessPerThread = 0;

        public WorkerThread(int tid, SpscLinkedQueue<FTSerializedState> queue, Status status) {
            this.tid = tid;
            this.queue = queue;
            this.status = status;
        }

        @Override
        public void run() {
            int count = 0;
            while (true) {
                if (isNoMoreInput && queue.isEmpty()) {
                    break;
                }
                FTSerializedState state = null;
                for (int i = 0; i < MAX_POLL_TIME; i++) {
                    state = queue.poll();
                    if (state != null) {
                        break;
                    }
                }
                if (state != null) {
                    history.add(state.getAddress(), state.isWrite(), state.getEvent(), state.getTicket(), state.getTid());
                    status.tackle();
                    count++;
                    if (count == 100) {
                        count = 0;
                        if (status.untackled() < MAX_QUEUE_SIZE / 5) {
                            synchronized (status) {
                                status.notify();
                            }
                        }
                    }
                    //tackledAccess.incrementAndGet();
//                    tackledAccessPerThread++;
//                    if (tackledAccessPerThread % 10000 == 0) {
//                        System.out.println("worker thread " + tid + "shadow memory size " + history.getEntryNum());
//                    }
                }
            }
            // System.out.println("worked thread " + tid + " tackled " + tackledAccessPerThread + " accesses");
        }
    }
}
