package backend;

import balok.causality.Epoch;
import balok.ser.SerializedFrame;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

public class FrameInput {

        private Kryo kryo;

        private List<Input> inputList;

        private ListIterator<Input> roundRobinIter;

        private boolean isEmpty;

        public FrameInput(Kryo kryo, List<Input> inputList) {
            this.kryo = kryo;
            this.inputList = inputList;
            // filter empty inputs
            ListIterator<Input> iter = inputList.listIterator();
            while (iter.hasNext()) {
                Input input = iter.next();
                if (input.eof()) {
                   input.close();
                   iter.remove();
                }
            }
            if (inputList.isEmpty()) {
                isEmpty = true;
            } else {
                isEmpty = false;
            }
            this.roundRobinIter = inputList.listIterator();
        }

        public Optional<SerializedFrame<Epoch>> getNextFrame() {
           if (isEmpty) {
               return Optional.empty();
           } else {
               Input input = roundRobinIter.next();
               Optional frame = Optional.of(kryo.readObject(input, SerializedFrame.class));
               if (input.eof()) {
                   input.close();
                   roundRobinIter.remove();
               }
               if (!roundRobinIter.hasNext()) {
                   if (inputList.isEmpty()) {
                       // no more input
                       isEmpty = true;
                   } else {
                       // go back to the head
                       roundRobinIter = inputList.listIterator();
                   }
               }
               return frame;
           }
        }
}
