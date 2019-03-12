package backend;

import balok.causality.Epoch;
import balok.causality.async.AsyncLocationTracker;
import balok.causality.async.ShadowMemory;
import balok.ser.SerializedFrame;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelFrameAnalyzer {
    private ShadowMemory<Epoch> history;

    private HashMap<Integer, AsyncLocationTracker<Epoch>> index;

    private ExecutorService pool;

    private int poolSize;

    public ParallelFrameAnalyzer(int poolSize) {
        this.history = new ShadowMemory<>();
        this.index = new HashMap<>();
        pool = Executors.newWorkStealingPool(poolSize);
    }

    public void addFrame(SerializedFrame<Epoch> frame) {

    }

    public void close() {
        pool.shutdown();
    }
}
