package backend;

import balok.causality.Epoch;
import balok.causality.async.AsyncLocationTracker;
import balok.causality.async.ShadowMemory;
import balok.causality.async.DataRacePolicy;
import balok.causality.AccessMode;
import com.carrotsearch.hppc.IntObjectHashMap;
import java.util.List;
import tools.balok.MemoryAccess;

public class MemoryAccessAnalyzer {
    private ShadowMemory<Epoch> history;

    private List<MemoryAccess> accesses;

    private IntObjectHashMap<AsyncLocationTracker<Epoch>> index;

    public MemoryAccessAnalyzer(List<MemoryAccess> accesses) {
        this.accesses = accesses;
        this.history = new ShadowMemory<>();
        this.index = new IntObjectHashMap<>(accesses.size() / 100);
    }

    public void doRaceDetection() {
        for (MemoryAccess access : accesses) {
            AsyncLocationTracker<Epoch> loc = index.getOrDefault(access.getAddress(), null);
            if (loc == null) {
                loc = new AsyncLocationTracker<Epoch>((mode1, ev1, mode2, ev2) -> {
                    // We log any data race
                    System.out.println("Race Detected!");
                    System.out.println("Access 1: " + mode1 + " " + ev1);
                    System.out.println("Access 2: " + mode2 + " " + ev2);
                    // We say to ADD in case of a write so that that access is written to the shadow
                    // location We say to DISCARD in the case of a read so that the operation can
                    // continue (ignoring reads) if a data-race occurs.
                    return mode1 == AccessMode.WRITE ? DataRacePolicy.ADD : DataRacePolicy.DISCARD;
                });
                index.put(access.getAddress(), loc);
            }
            history.add(loc, access.getMode(), access.getVC(), access.getTicket());
        }
    }
}
