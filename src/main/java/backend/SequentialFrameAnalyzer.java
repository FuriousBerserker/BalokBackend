package backend;

import backend.util.IntervalTree;
import backend.util.Segment;
import backend.util.SimpleShadowMemory;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import tools.fasttrack_frontend.FTSerializedState;

public class SequentialFrameAnalyzer {
    
    private SimpleShadowMemory history = new SimpleShadowMemory();;

    private long tackledAccess = 0L;

    private boolean doSanityCheck;

    public SequentialFrameAnalyzer() {
        this(false);
    }

    public SequentialFrameAnalyzer(boolean doSanityCheck) {
        this.doSanityCheck = doSanityCheck;
    }

    public void addFrame(FTSerializedState[] frame) {
        for (int i = 0; i < frame.length; i++) {
            FTSerializedState state = frame[i];
//            System.out.println(state);
//            System.out.println("================================");
            history.add(state.getAddress(), state.isWrite(), state.getEvent(), state.getTicket(), state.getTid());
        }
        tackledAccess += frame.length;
    }


    public void close() {
        if (doSanityCheck) {
            sanityCheck();
        } else {
            System.out.println("no sanity check");
        }
        System.out.println("tackled memory access: " + tackledAccess);
    }

    public void sanityCheck() {
        int errorNum = 0;
        int memoryLocations = 0;
        for (IntObjectCursor<IntervalTree<Segment>> cursor : history) {
            memoryLocations += 1;
            if (cursor.value.size() != 1) {
                errorNum += 1;
                System.out.println(cursor.key + " final size is " + cursor.value.size());
                System.out.println(cursor.value);
            }
        }
        System.out.println("total number of memory locations is " + memoryLocations);
        System.out.println("total number of unaccomplished memory locations is " + errorNum);
    }
}
