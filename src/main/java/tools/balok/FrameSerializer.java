package tools.balok;

import balok.causality.*;
import balok.causality.async.AsyncLocationTracker;
import balok.ser.SerializedFrame;
import balok.ser.TaskViewSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class FrameSerializer extends Serializer<SerializedFrame<Epoch>> {
    @Override
    public void write(Kryo kryo, Output output, SerializedFrame<Epoch> frame) {
        // output size
        output.writeInt(frame.size());
        // output loc
        for (int address : frame.getAddresses()) {
            output.writeInt(address);
        }
        // output mode
        for (AccessMode mode : frame.getModes()) {
            output.writeBoolean(mode == AccessMode.READ ? true : false);
        }

        // output event
        for (Event<Epoch> event : frame.getEvents()) {
            TaskViewSerializer.writeTaskView((i) -> output.writeInt(i), (TaskView)event);
        }
        // output ticket
        for (int ticket : frame.getTickets()) {
            output.writeInt(ticket);
        }

    }

    @Override
    public SerializedFrame<Epoch> read(Kryo kryo, Input input, Class<SerializedFrame<Epoch>> aClass) {
        // input size
        int size = input.readInt();
        int[] addresses = new int[size];
        AccessMode[] modes = new AccessMode[size];
        Event[] events = new Event[size];
        int[] tickets = new int[size];

        //input loc
        for (int i = 0; i < size; i++) {
            addresses[i] = input.readInt();
        }

        //input mode
        for (int i = 0; i < size; i++) {
            modes[i] = (input.readBoolean() ? AccessMode.READ : AccessMode.WRITE);
        }

        //input event
        for (int i = 0; i < size; i++) {
            events[i] = TaskViewSerializer.readTaskView(PtpCausalityFactory.VECTOR_MUT, () -> input.readInt());
        }

        //input ticket
        for (int i = 0; i < size; i++) {
            tickets[i] = input.readInt();
        }
        return new SerializedFrame<Epoch>(addresses, modes, events, tickets, size);
    }
}
