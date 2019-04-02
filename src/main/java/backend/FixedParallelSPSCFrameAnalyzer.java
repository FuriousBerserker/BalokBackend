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
//        System.out.println("Race Detection Complete");
//        try {
//            Thread.sleep(Integer.MAX_VALUE);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        System.out.println("tackled memory access: " + tackledAccess.get());
    }

    public AtomicLong getTackledAccess() {
        return tackledAccess;
    }

    class WorkerThread implements Runnable {

        private int tid;

        private SpscLinkedQueue<MemoryAccess> queue;

        private Status status;

        private SimpleShadowMemory history = new SimpleShadowMemory(SparseShadowEntry::new);

        private int tackledAccessPerThread = 0;

        public WorkerThread(int tid, SpscLinkedQueue<MemoryAccess> queue, Status status) {
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
                MemoryAccess access = null;
                for (int i = 0; i < MAX_POLL_TIME; i++) {
                    access = queue.poll();
                    if (access != null) {
                        break;
                    }
                }
                if (access != null) {
                    history.add(access.getAddress(), access.getMode(), access.getEvent(), access.getTicket());
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
