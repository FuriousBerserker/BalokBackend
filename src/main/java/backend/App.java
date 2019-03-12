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
import tools.balok.FrameSerializer;
import balok.ser.SerializedFrame;
import balok.causality.Epoch;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;

public class App {
    
    public String getGreeting() {
        return "app should have a greeting";
    }

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        Option silent = new Option("s", false, "do not carry out race detection");
        Option time = new Option("t", false, "measure execution time");
        Option localMerge = new Option("m", false, "enable local merging optimization");
        options.addOption(silent);
        options.addOption(time);
        options.addOption(localMerge);
        CommandLine line = null;
        HelpFormatter help = new HelpFormatter();
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            help.printHelp("java App", options, true);
            System.exit(1);
        }


        // code that handle MemoryAccess objects
        /* ArrayList<MemoryAccess> accesses = new ArrayList<>();
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
        analyzer.doRaceDetection(); */
        long start = 0l;
        if (line.hasOption(time.getOpt())) {
            start = System.currentTimeMillis();
        }
        Kryo kryo = new Kryo();
        kryo.setReferences(false);
        kryo.setRegistrationRequired(true);
        kryo.register(SerializedFrame.class, new FrameSerializer());
        long accessNum = 0;
        FrameAnalyzer analyzer = new FrameAnalyzer();
        if (line.getArgs().length == 0) {
            System.out.println("Please input log file");
            help.printHelp("java App", options, true);
            System.exit(1);
        }
        String logFile = line.getArgs()[0];
        try {
            Input input = new Input(new GZIPInputStream(new FileInputStream(logFile)));
            SerializedFrame<Epoch> frame = null;
            if (line.hasOption(silent.getOpt())) {
                while (!input.eof()) {
                    frame = kryo.readObject(input, SerializedFrame.class);
                    accessNum += frame.size();
                }
            } else if (line.hasOption(localMerge.getOpt())) {
                while (!input.eof()) {
                    frame = kryo.readObject(input, SerializedFrame.class);
                    accessNum += frame.size();
                    analyzer.addFrameWithLocalMerge(frame);
                }
            } else {
                while (!input.eof()) {
                    frame = kryo.readObject(input, SerializedFrame.class);
                    accessNum += frame.size();
                    analyzer.addFrame(frame);
                }
            }
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long elapsedTime = 0l;
        if (line.hasOption(time.getOpt())) {
            elapsedTime = System.currentTimeMillis() - start;
            System.out.println("Elapsed Time: " + elapsedTime + " ms");
        }
        System.out.println("Number of memory accesses: " + accessNum);
    }
}
