package backend;

import balok.causality.AccessMode;
import balok.causality.Epoch;
import balok.causality.async.*;
import balok.ser.SerializedFrame;
import org.jctools.queues.SpscLinkedQueue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FixedParallelSPSCFrameAnalyzer {

    private final int workerThreadNum;

    private SpscLinkedQueue<MemoryAccess>[] queues;

    private Thread[] workerThreads;

    private volatile boolean isNoMoreInput = false;

    private AtomicLong tackledAccess = new AtomicLong(0L);

    private static int MAX_POLL_TIME = 10;

    public FixedParallelSPSCFrameAnalyzer(int workerThreadNum) {
        this.workerThreadNum = workerThreadNum;
        queues = new SpscLinkedQueue[workerThreadNum];
        workerThreads = new Thread[workerThreadNum];
        for (int i = 0; i < workerThreadNum; i++) {
            queues[i] = new SpscLinkedQueue<>();
            workerThreads[i] = new Thread(new WorkerThread(i, queues[i]));
            workerThreads[i].start();
        }
    }

    public void addFrame(SerializedFrame<Epoch> frame) {
        for (int i = 0; i < frame.size(); i++) {
            int address = frame.getAddresses()[i];
            // partition memory accesses in a round-robin manner
            int workerThreadTid = address % workerThreadNum;
            if (workerThreadTid < 0) {
                workerThreadTid = -workerThreadTid;
            }
            MemoryAccess access = new MemoryAccess(address, frame.getModes()[i], frame.getEvents()[i], frame.getTickets()[i]);
            queues[workerThreadTid].offer(access);
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

        private SpscLinkedQueue<MemoryAccess> queue;

        private SimpleShadowMemory history = new SimpleShadowMemory(SparseShadowEntry::new);

        public WorkerThread(int tid, SpscLinkedQueue<MemoryAccess> queue) {
            this.tid = tid;
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                if (isNoMoreInput && queue.isEmpty()) {
                    break;
                }
                MemoryAccess access = null;
                for (int i = 0; i < MAX_POLL_TIME; i++) {
                    access = queue.poll();
                    if (access != null) {
                        break;
                    }
                }
                if (access != null) {
                    history.add(access.getAddress(), access.getMode(), access.getEvent(), access.getTicket());
                    tackledAccess.incrementAndGet();
                }
            }
        }
    }
}
