package backend;

import balok.causality.AccessMode;
import balok.causality.Epoch;
import balok.causality.TaskId;
import balok.causality.async.*;
import balok.ser.SerializedFrame;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class ParallelFrameAnalyzer {

    private static final int THRESHOLD_TO_SUBMIT = 10000;

    private HashMap<Integer, AsyncLocationTracker<Epoch>> locMap = new HashMap<>();

    private ExecutorService pool;

    private int poolSize;

    private Supplier<ShadowEntry<Epoch>> factory = SparseShadowEntry::new;

    private ShadowMemory<Epoch> history = new ShadowMemory<>(factory);

    public ParallelFrameAnalyzer(int poolSize) {
        this.poolSize = poolSize;
        this.pool = Executors.newWorkStealingPool(poolSize);
        //this.pool = Executors.newFixedThreadPool(poolSize);
    }

    public void addFrame(SerializedFrame<Epoch> frame) {
        HashMap<Integer, LinkedList<Integer>> indexListMap = new HashMap<>();
        int[] addresses = frame.getAddresses();
        // get index list for each memory location
        for (int i = 0; i < frame.size(); i++) {
            LinkedList<Integer> indexList = indexListMap.getOrDefault(addresses[i], null);
            if (indexList == null) {
                indexList = new LinkedList<>();
                indexListMap.put(addresses[i], indexList);
            }
            indexList.add(i);
        }

        LinkedList<Future<Object>> futureList = new LinkedList<>();
        LinkedList<AsyncLocationTracker<Epoch>> locList = new LinkedList<>();
        LinkedList<ShadowEntry<Epoch>> shadowEntryList = new LinkedList<>();
        int totalSize = 0;
        for (Map.Entry<Integer, LinkedList<Integer>> entry : indexListMap.entrySet()) {
            AsyncLocationTracker<Epoch> loc = locMap.getOrDefault(entry.getKey(), null);
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
                locMap.put(entry.getKey(), loc);
            }
            ShadowEntry<Epoch> shadowEntry = history.addWithoutRefresh(loc, frame.getModes(), frame.getEvents(), frame.getTickets(), entry.getValue());
            int entrySize = shadowEntry.size();
            if (entrySize >= THRESHOLD_TO_SUBMIT) {
                RefreshTask<Epoch> task = new RefreshTask<>(loc, shadowEntry);
                futureList.add(pool.submit(task));
            } else {
                locList.add(loc);
                shadowEntryList.add(shadowEntry);
                totalSize += entrySize;
                if (totalSize >= THRESHOLD_TO_SUBMIT) {
                    BatchRefreshTask<Epoch> batchTask = new BatchRefreshTask<>(locList, shadowEntryList);
                    futureList.add(pool.submit(batchTask));
                    locList = new LinkedList<>();
                    shadowEntryList = new LinkedList<>();
                    totalSize = 0;
                }
            }
        }
        try {
            for (Future<Object> future : futureList) {
                future.get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    public void close() {
        LinkedList<AsyncLocationTracker<Epoch>> locList = new LinkedList<>();
        LinkedList<ShadowEntry<Epoch>> shadowEntryList = new LinkedList<>();
        LinkedList<Future<Object>> futureList = new LinkedList<>();

        int totalSize = 0;
        for (AsyncLocationTracker<Epoch> loc : locMap.values()) {
            ShadowEntry<Epoch> shadowEntry = history.getShadowEntry(loc);
            if (shadowEntry.size() == 1) {
                continue;
            }
            locList.add(loc);
            shadowEntryList.add(shadowEntry);
            totalSize += shadowEntry.size();
            if (totalSize >= THRESHOLD_TO_SUBMIT) {
                BatchRefreshTask<Epoch> task = new BatchRefreshTask<>(locList, shadowEntryList);
                futureList.add(pool.submit(task));
                locList = new LinkedList<>();
                shadowEntryList = new LinkedList<>();
                totalSize = 0;
            }
        }
        if (totalSize > 0) {
            BatchRefreshTask<Epoch> task = new BatchRefreshTask<>(locList, shadowEntryList);
            futureList.add(pool.submit(task));
        }

        try {
            for (Future<Object> future : futureList) {
                future.get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        pool.shutdown();
    }

    static class RefreshTask<T extends TaskId> implements Callable<Object> {

        private AsyncLocationTracker<T> loc;

        private ShadowEntry<T> entry;

        RefreshTask(AsyncLocationTracker<T> loc, ShadowEntry<T> entry) {
            this.loc = loc;
            this.entry = entry;
        }

        @Override
        public Object call() throws Exception {
            entry.refresh(loc);
            return null;
        }
    }

    static class BatchRefreshTask<T extends TaskId> implements Callable<Object> {

        private List<AsyncLocationTracker<T>> locList;

        private List<ShadowEntry<T>> shadowEntryList;

        BatchRefreshTask(List<AsyncLocationTracker<T>> locList, List<ShadowEntry<T>> shadowEntryList) {
            this.locList = locList;
            this.shadowEntryList = shadowEntryList;
        }

        @Override
        public Object call() throws Exception {
            assert (locList.size() == shadowEntryList.size());
            Iterator<AsyncLocationTracker<T>> iter1 = locList.iterator();
            Iterator<ShadowEntry<T>> iter2 = shadowEntryList.iterator();
            while (iter1.hasNext()) {
                ShadowEntry<T> shadowEntry = iter2.next();
                AsyncLocationTracker<T> loc = iter1.next();
                shadowEntry.refresh(loc);
            }
            return null;
        }
    }
}
