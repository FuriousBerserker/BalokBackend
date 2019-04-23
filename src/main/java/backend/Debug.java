package backend;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntLongHashMap;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.cursors.IntLongCursor;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import tools.fasttrack_frontend.FTSerializedState;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

public class Debug {

    private String benchmarkName;

    //private IntLongHashMap accessDistribution = new IntLongHashMap();

    private IntObjectHashMap<IntArrayList> accessMap = new IntObjectHashMap<>();



//    private HashSet<Integer> accessMap = new HashSet<>();

    public Debug(String benchmarkName) {
        this.benchmarkName = benchmarkName;
//        Path path = Paths.get("address.txt");
//        Charset charset = Charset.forName("utf-8");
//        try {
//            for (String str : Files.readAllLines(path, charset)) {
//                accessMap.add(Integer.parseInt(str));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void addFrame(FTSerializedState[] frame) {
//        for (int i = 0; i < frame.length; i++) {
//            long numOfAccess = accessDistribution.getOrDefault(frame[i].getAddress(), 0L) + 1L;
//            accessDistribution.put(frame[i].getAddress(), numOfAccess);
//            if (accessMap.contains(frame.getAddresses()[i])) {
//                System.out.println(frame.getAddresses()[i] + " " + frame.getTickets()[i] + " " + frame.getModes()[i].name());
//            }
//        }
        for (FTSerializedState state : frame) {
            IntArrayList l = accessMap.get(state.getAddress());
            if (l == null) {
                l = new IntArrayList();
                accessMap.put(state.getAddress(), l);
            }
            l.add(state.getTicket());
        }
    }

//    public void findSingleAccessLocation() {
//        for (IntLongCursor cursor : accessDistribution) {
//            if (cursor.value == 1L) {
//                System.out.println("memory location " + cursor.key + " has only one memory access");
//            }
//        }
//    }

    public void debug() {
        //IntIntHashMap unconnectedIntervals = new IntIntHashMap();
        int max = 0;
        int min = Integer.MAX_VALUE;
        int total = 0;
        int outputLocatons = 0;
        for (IntObjectCursor<IntArrayList> cursor : accessMap) {
            int[] buffer = cursor.value.buffer;
            int size = cursor.value.size();
            Arrays.sort(buffer, 0, size);
            if (outputLocatons < 5) {
                System.out.println(cursor.key + "->" + Arrays.toString(Arrays.copyOf(buffer, 10)));
                outputLocatons++;
            }
            int unconnected = 0;
            for (int i = 1; i < size; i++) {
                if (buffer[i] != buffer[i - 1] + 1) {
                    unconnected++;
                }
            }
            if (max < unconnected) {
                max = unconnected;
            }
            if (min > unconnected) {
                min = unconnected;
            }
            total += unconnected;
        }
        System.out.println("Min disconnected interval per location: " + min);
        System.out.println("Max disconnected interval per location: " + max);
        System.out.println("Total disconnected interval: " + total);
    }
}
