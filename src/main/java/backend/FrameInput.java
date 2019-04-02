package backend;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import tools.fasttrack_frontend.FTSerializedState;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

public class FrameInput {

        private Kryo kryo;

        private List<Input> inputList;

        private ListIterator<Input> roundRobinIter;

        private boolean isEmpty;

        private final int MAX_FRAME_SIZE;

        public FrameInput(Kryo kryo, List<Input> inputList, int frameSize) {
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
            this.MAX_FRAME_SIZE = frameSize;
        }

        public Optional<FTSerializedState[]> getNextFrame() {
           if (isEmpty) {
               return Optional.empty();
           } else {
               Input input = roundRobinIter.next();
               FTSerializedState[] frame = new FTSerializedState[MAX_FRAME_SIZE];
               int count = 0;
               while (count < MAX_FRAME_SIZE && !input.eof()) {
                   frame[count++] = kryo.readObject(input, FTSerializedState.class);
               }

               if (input.eof()) {
                   input.close();
                   roundRobinIter.remove();
                   frame = Arrays.copyOfRange(frame, 0, count);
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
               return Optional.of(frame);
           }
        }
}
