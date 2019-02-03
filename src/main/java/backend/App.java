/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package backend;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.ArrayList;
import tools.balok.MemoryAccess;

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
        try {
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(logFile));
            accesses = (ArrayList<MemoryAccess>) input.readObject();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
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