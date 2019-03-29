package backend;

import balok.causality.Epoch;
import balok.ser.SerializedFrame;
import com.carrotsearch.hppc.IntLongHashMap;
import com.carrotsearch.hppc.cursors.IntLongCursor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

public class Debug {

    private String benchmarkName;

    private IntLongHashMap accessDistribution = new IntLongHashMap();

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

    public void addFrame(SerializedFrame<Epoch> frame) {
        for (int i = 0; i < frame.size(); i++) {
            long numOfAccess = accessDistribution.getOrDefault(frame.getAddresses()[i], 0L) + 1L;
            accessDistribution.put(frame.getAddresses()[i], numOfAccess);
//            if (accessMap.contains(frame.getAddresses()[i])) {
//                System.out.println(frame.getAddresses()[i] + " " + frame.getTickets()[i] + " " + frame.getModes()[i].name());
//            }
        }
    }

    public void findSingleAccessLocation() {
        for (IntLongCursor cursor : accessDistribution) {
            if (cursor.value == 1L) {
                System.out.println("memory location " + cursor.key + " has only one memory access");
            }
        }
    }
}
