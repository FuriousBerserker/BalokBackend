package backend;

import balok.causality.AccessMode;
import balok.causality.Epoch;
import balok.causality.async.AsyncLocationTracker;
import balok.causality.async.DataRacePolicy;
import balok.causality.async.ShadowMemory;
import balok.causality.async.SparseShadowEntry;
import balok.ser.SerializedFrame;
import it.unimi.dsi.fastutil.Hash;
import javafx.scene.effect.Shadow;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FixedParallelFrameAnalyzer {

    private final int workerThreadNum;

    private BlockingQueue<MemoryAccess>[] queues;

    private Thread[] workerThreads;

    private volatile boolean isNoMoreInput = false;

    private ConcurrentHashMap<Integer, AsyncLocationTracker<Epoch>> locMap = new ConcurrentHashMap<>(1000000);

    private AtomicLong tackledAccess = new AtomicLong(0L);

    public FixedParallelFrameAnalyzer(int workerThreadNum) {
        this.workerThreadNum = workerThreadNum;
        queues = new LinkedBlockingQueue[workerThreadNum];
        workerThreads = new Thread[workerThreadNum];
        for (int i = 0; i < workerThreadNum; i++) {
            queues[i] = new LinkedBlockingQueue<>();
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

    class WorkerThread implements Runnable {

        private int tid;

        private BlockingQueue<MemoryAccess> queue;

        private ShadowMemory history = new ShadowMemory(SparseShadowEntry::new);

        public WorkerThread(int tid, BlockingQueue<MemoryAccess> queue) {
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
                    MemoryAccess access = queue.poll(1000, TimeUnit.MILLISECONDS);
                    if (access != null) {
                        int address = access.getAddress();
                        AsyncLocationTracker<Epoch> loc = locMap.getOrDefault(address, null);
                        if (loc == null) {
                            loc = new AsyncLocationTracker<>((mode1, ev1, mode2, ev2) -> {
                                // We log any data race
                                System.out.println("Race Detected!");
                                System.out.println("Access 1: " + mode1 + " " + ev1);
                                System.out.println("Access 2: " + mode2 + " " + ev2);
                                // We say to ADD in case of a write so that that access is written to the shadow
                                // location We say to DISCARD in the case of a read so that the operation can
                                // continue (ignoring reads) if a data-race occurs.
                                return mode1 == AccessMode.WRITE ? DataRacePolicy.ADD : DataRacePolicy.DISCARD;
                            });
                            locMap.put(address, loc);
                        }
                        history.add(loc, access.getMode(), access.getEvent(), access.getTicket());
                        tackledAccess.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
