package backend;

import balok.ser.SerializedFrame;
import balok.causality.Epoch;
import balok.causality.Event;
import balok.causality.async.SparseShadowEntry;
import balok.causality.async.AsyncLocationTracker;
import balok.causality.async.ShadowMemory;
import balok.causality.async.DataRacePolicy;
import balok.causality.async.Frame;
import balok.causality.async.FrameBuilder;
import balok.causality.AccessMode;
import com.carrotsearch.hppc.IntObjectHashMap;
import java.util.HashMap;

public class FrameAnalyzer {
    
    private ShadowMemory<Epoch> history;

    private HashMap<Integer, AsyncLocationTracker<Epoch>> index;

    public FrameAnalyzer() {
        this.history = new ShadowMemory<>(SparseShadowEntry::new);
        this.index = new HashMap<>();
    }

    public void addFrame(SerializedFrame<Epoch> frame) {
        for (int address : frame.getAddresses()) {
            AsyncLocationTracker<Epoch> loc = index.getOrDefault(address, null);
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
                index.put(address, loc);
            }
        } 
        frame.addTo(index, history);
    }

    public void addFrameWithLocalMerge(SerializedFrame<Epoch> frame) {
        int[] addresses = frame.getAddresses();
        Event<Epoch>[] events = frame.getEvents();
        AccessMode[] modes = frame.getModes();
        int[] tickets = frame.getTickets();

        for (int i = 0; i < frame.size(); i++) {
            AsyncLocationTracker<Epoch> loc = index.getOrDefault(addresses[i], null);
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
                index.put(addresses[i], loc);
            }

            if (!loc.tryAdd(modes[i], events[i], tickets[i])) {
               history.add(loc, modes[i], events[i], tickets[i]); 
            }
        }
    }
}
