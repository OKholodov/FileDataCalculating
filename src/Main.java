import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        //final int filesToThread = 100;
        final int numThreads = 5;
        EnumLogModes logMode = EnumLogModes.MED;

        Scanner inDir = new Scanner(System.in);
        String path;
        String logFilePath;
        String resultFilePath;

        System.out.println("Enter the path to work");
        path = inDir.next();
        //path = "C:/JavaTest/test";
        File dir = new File(path);

        if (!dir.isDirectory()) {
            System.out.println("Wrong directory");
            return;
        }

        resultFilePath = path + "/result.dat";
        logFilePath = path + "/log.dat";
/*
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.startsWith("in_") && name.endsWith(".dat"));
            }
        });
*/
        File[] files = dir.listFiles((dir1, name) -> (name.startsWith("in_") && name.endsWith(".dat")));

        if (files == null || files.length == 0) {
            System.out.println("There are no files to process");
            return;
        }

        int cntFiles = files.length;

        // Creating Map to mark files (need only for logging)
        Map<File, String> hashmap = new HashMap<>();

        // Adding files to processing into the queue and Map
        for (File f: files) {
            CalculateFile.addFileToQueue(f);
            hashmap.put(f,"0");
        }

        //Delete and create file with results
        File resultFile = new File(resultFilePath);
        if (resultFile.exists()) {
            if(resultFile.delete()) {
                try {
                    if (resultFile.createNewFile()) {
                        try (RandomAccessFile raf = new RandomAccessFile(resultFile, "rw")) {
                            raf.seek(0);
                            raf.writeDouble(0.0);
                        }
                    }
                    else {
                        System.out.println("File results wasn't created!");
                    }
                } catch (IOException e) {
                    System.out.println("File results wasn't created!");
                    e.printStackTrace();
                    return;
                }
            }
            else {
                System.out.println("File with results wasn't re-created");
                return;
            }
        }

        //Print start time
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        Calendar cal = Calendar.getInstance();
        System.out.println(dateFormat.format(cal.getTime()));

        CalculateFile.logMode = logMode;
        CalculateFile.hashmap = hashmap;
        CalculateFile.resultFile = resultFile;

        // Generate threads
        /*
        for (int i=0; i<cntFiles/filesToThread; i++) {
            new CalculateFile();
        }
        */

        // Generate threads
        for (int i=0; i < numThreads; i++) {
            new CalculateFile();
        }

        //Waiting for terminated all child threads
        for (Thread th: CalculateFile.getThreads()) {
            try {
                if (th.isAlive()) {
                    th.join();
                    //System.out.println("Thread " + th.getId() + " joined.");
                }
            }
            catch (InterruptedException e) {
                System.out.println("Error in joining to thread. Thread id = " + th.getId());
            }
        }

        //Print end time
        cal = Calendar.getInstance();
        System.out.println(dateFormat.format(cal.getTimeInMillis()));

        System.out.println("End Main thread. Processed files: " + CalculateFile.getCntProcessed());

        //Create log file
        try (BufferedWriter bf = new BufferedWriter(new FileWriter(new File(logFilePath)))) {
            for (Map.Entry<File, String> entry: hashmap.entrySet()) {
                bf.write(entry.getKey() + " = " + entry.getValue());
                bf.newLine();
            }
        } catch (java.io.IOException e) {
            System.out.println("IE Exception");
        }

        //Print result from result file
        try (RandomAccessFile raf = new RandomAccessFile(resultFile, "r")) {
            raf.seek(0);
            System.out.println("Result = " + raf.readDouble());
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        catch (IOException e) {
            System.out.println("IO exception");
        }

    }
}
