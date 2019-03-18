package backend;

import balok.causality.AccessMode;
import balok.causality.Epoch;
import balok.causality.Event;

public class MemoryAccess {

    private int address;

    private AccessMode mode;

    private Event<Epoch> event;

    private int ticket;

    public MemoryAccess(int address, AccessMode mode, Event<Epoch> event, int ticket) {
        this.address = address;
        this.mode = mode;
        this.event = event;
        this.ticket = ticket;
    }

    public int getAddress() {
        return address;
    }

    public AccessMode getMode() {
        return mode;
    }

    public Event<Epoch> getEvent() {
        return event;
    }

    public int getTicket() {
        return ticket;
    }
}
