package backend;

import balok.causality.async.*;
import balok.ser.SerializedFrame;
import balok.causality.Epoch;
import balok.causality.Event;
import balok.causality.AccessMode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SequentialFrameAnalyzer {
    
    private SimpleShadowMemory<Epoch> history = new SimpleShadowMemory<>(SparseShadowEntry::new);;

    private long tackledAccess = 0L;

    private boolean doSanityCheck;

    private HashSet<Integer> addressSet;

    public SequentialFrameAnalyzer() {
        this(false);
    }

    public SequentialFrameAnalyzer(boolean doSanityCheck) {
        this.doSanityCheck = doSanityCheck;
        if (this.doSanityCheck) {
            this.addressSet = new HashSet<>();
        }
    }

    public void addFrame(SerializedFrame<Epoch> frame) {
        for (int i = 0; i < frame.size(); i++) {
            history.add(frame.getAddresses()[i], frame.getModes()[i], frame.getEvents()[i], frame.getTickets()[i]);
            if (doSanityCheck) {
                addressSet.add(frame.getAddresses()[i]);
            }
        }
        tackledAccess += frame.size();
    }

    public void addFrameByMemoryAccess(SerializedFrame<Epoch> frame) {
        for (int i = 0; i < frame.size(); i++) {
            MemoryAccess access = new MemoryAccess(frame.getAddresses()[i], frame.getModes()[i], frame.getEvents()[i], frame.getTickets()[i]);
//            if (access.getTicket() == -2147483594) {
//                System.out.println("before");
//                System.out.println(history.getShadowEntry(loc));
//            }
            history.add(access.getAddress(), access.getMode(), access.getEvent(), access.getTicket());
            if (doSanityCheck) {
                addressSet.add(access.getAddress());
            }
            tackledAccess++;
//            if (access.getTicket() == -2147483594) {
//                System.out.println("after");
//                System.out.println(history.getShadowEntry(loc));
//            }
        }
    }

//    public void addFrameWithLocalMerge(SerializedFrame<Epoch> frame) {
//        int[] addresses = frame.getAddresses();
//        Event<Epoch>[] events = frame.getEvents();
//        AccessMode[] modes = frame.getModes();
//        int[] tickets = frame.getTickets();
//
//        for (int i = 0; i < frame.size(); i++) {
//            AsyncLocationTracker<Epoch> loc = locMap.getOrDefault(addresses[i], null);
//            if (loc == null) {
//                loc = new AsyncLocationTracker<Epoch>((mode1, ev1, mode2, ev2) -> {
//                    // We log any data race
//                    System.out.println("Race Detected!");
//                    System.out.println("Access 1: " + mode1 + " " + ev1);
//                    System.out.println("Access 2: " + mode2 + " " + ev2);
//                    // We say to ADD in case of a write so that that access is written to the shadow
//                    // location We say to DISCARD in the case of a read so that the operation can
//                    // continue (ignoring reads) if a data-race occurs.
//                    return mode1 == AccessMode.WRITE ? DataRacePolicy.ADD : DataRacePolicy.DISCARD;
//                });
//                locMap.put(addresses[i], loc);
//            }
//            if (!loc.tryAdd(modes[i], events[i], tickets[i])) {
//               history.add(loc, modes[i], events[i], tickets[i]);
//            }
//            tackledAccess++;
//        }
//    }

    public void close() {
        System.out.println("tackled memory access: " + tackledAccess);
    }

    public void sanityCheck() {
        if (doSanityCheck) {
            int errorNum = 0;
            for (int address : addressSet) {
                ShadowEntry<Epoch> entry = history.get(address);
                if (entry.size() != 1) {
                    errorNum += 1;
                    System.out.println(address + " final size is " + entry.size());
                    System.out.println(entry);
                }
            }
            System.out.println("total number of unaccomplished memory locations is " + errorNum);
        } else {
            System.out.println("no sanity check");
        }
    }
}
