package backend;

import balok.causality.async.*;
import balok.ser.SerializedFrame;
import balok.causality.Epoch;
import balok.causality.Event;
import balok.causality.AccessMode;

import java.util.HashMap;
import java.util.Map;

public class SequentialFrameAnalyzer {
    
    private ShadowMemory<Epoch> history;

    private HashMap<Integer, AsyncLocationTracker<Epoch>> locMap;

    private long tackledAccess = 0L;

    public SequentialFrameAnalyzer() {
        this.history = new ShadowMemory<>(SparseShadowEntry::new);
        this.locMap = new HashMap<>();
    }

    public void addFrame(SerializedFrame<Epoch> frame) {
        for (int address : frame.getAddresses()) {
            AsyncLocationTracker<Epoch> loc = locMap.getOrDefault(address, null);
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
                locMap.put(address, loc);
            }
        }
        frame.addTo(locMap, history);
        tackledAccess += frame.size();
    }

    public void addFrameByMemoryAccess(SerializedFrame<Epoch> frame) {
        for (int i = 0; i < frame.size(); i++) {
            int address = frame.getAddresses()[i];
            AsyncLocationTracker<Epoch> loc = locMap.getOrDefault(address, null);
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
                locMap.put(address, loc);
            }
            MemoryAccess access = new MemoryAccess(address, frame.getModes()[i], frame.getEvents()[i], frame.getTickets()[i]);
//            if (access.getTicket() == -2147483594) {
//                System.out.println("before");
//                System.out.println(history.getShadowEntry(loc));
//            }
            history.add(loc, access.getMode(), access.getEvent(), access.getTicket());
            tackledAccess++;
//            if (access.getTicket() == -2147483594) {
//                System.out.println("after");
//                System.out.println(history.getShadowEntry(loc));
//            }
        }
    }

    public void addFrameWithLocalMerge(SerializedFrame<Epoch> frame) {
        int[] addresses = frame.getAddresses();
        Event<Epoch>[] events = frame.getEvents();
        AccessMode[] modes = frame.getModes();
        int[] tickets = frame.getTickets();

        for (int i = 0; i < frame.size(); i++) {
            AsyncLocationTracker<Epoch> loc = locMap.getOrDefault(addresses[i], null);
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
                locMap.put(addresses[i], loc);
            }
            if (!loc.tryAdd(modes[i], events[i], tickets[i])) {
               history.add(loc, modes[i], events[i], tickets[i]); 
            }
            tackledAccess++;
        }
    }

    public void close() {
        System.out.println("tackled memory access: " + tackledAccess);
    }

    public void sanityCheck() {
        int errorNum = 0;
        for (Map.Entry<Integer, AsyncLocationTracker<Epoch>> loc : locMap.entrySet()) {
            ShadowEntry<Epoch> entry = history.getShadowEntry(loc.getValue());
            if (entry.size() != 1) {
                errorNum += 1;
                System.out.println(loc.getKey() + " final size is " + entry.size());
                System.out.println(entry);
            }
        }
        System.out.println("total number of unaccomplished memory locations is " + errorNum);
    }
}
