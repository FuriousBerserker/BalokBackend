/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package backend;

import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.util.List;
import java.util.ArrayList;
import tools.balok.MemoryAccess;
import tools.balok.MemoryAccessSerializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

public class App {
    
    public String getGreeting() {
        return "app should have a greeting";
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("java App <log file>");
            System.exit(0);
        }
        String logFile = args[0];
        ArrayList<MemoryAccess> accesses = new ArrayList<>();
        Kryo kryo = new Kryo();
        kryo.setReferences(false);
        kryo.register(MemoryAccess.class, new MemoryAccessSerializer());
        try {
            Input input = new Input(new GZIPInputStream(new FileInputStream(logFile)));
            MemoryAccess access = null;
            while (!input.eof()) {
                access = kryo.readObject(input, MemoryAccess.class);
                accesses.add(access);
            }
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Number of memory accesses: " + accesses.size());
        //for (MemoryAccess access: accesses) {
            //System.out.println(access.getAddress() + " " + access.getFile() + " " + access.getLine());
        //}
        MemoryAccessAnalyzer analyzer = new MemoryAccessAnalyzer(accesses);
        analyzer.doRaceDetection();
    }
}
