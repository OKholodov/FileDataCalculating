import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner inDir = new Scanner(System.in);
        String path;
        final int filesToThread = 15;
        String logFilePath;
        String resultFilePath;

        System.out.println("Enter the path to work");
        //path = inDir.next();
        path = "C:/JavaTest";
        File dir = new File(path);

        if (!dir.isDirectory()) {
            System.out.println("Wrong directory");
            return;
        }

        resultFilePath = path + "/result.dat";
        logFilePath = path + "/log.dat";

        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.startsWith("in_") && name.endsWith(".dat"));
            }
        });

        if (files.length == 0) {
            System.out.println("There are no files to process");
            return;
        }

        int cntFiles = files.length;

        Map<File, String> hashmap = new HashMap<File, String>();

        // Adding files to processing into the queue
        for (File f: files) {
            CalculateFile.addFileToQueue(f);
            hashmap.put(f,"0");
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        Calendar cal = Calendar.getInstance();
        System.out.println(dateFormat.format(cal.getTime()));

        // Generate threads
        for (int i=0; i<cntFiles/filesToThread; i++) {
            new CalculateFile(false, hashmap, resultFilePath);
        }

        for (Thread th: CalculateFile.getThreads()) {
            try {
                if (th.isAlive()) {
                    th.join();
                    System.out.println("Thread " + th.getId() + " joined.");
                }
            }
            catch (InterruptedException e) {
                System.out.println("Error in joining to thread");
            }
        }

        cal = Calendar.getInstance();
        System.out.println(dateFormat.format(cal.getTimeInMillis()));

        System.out.println("End Main thread. Processed files: " + CalculateFile.getCntProcessed());

        try (BufferedWriter bf = new BufferedWriter(new FileWriter(new File(logFilePath)))) {
            for (Map.Entry<File, String> entry: hashmap.entrySet()) {
                bf.write(entry.getKey() + " = " + entry.getValue());
                bf.newLine();
            }
        } catch (java.io.IOException e) {
            System.out.println("IE Exception");
        }

/*
        boolean isParsed;
        double res;

        for (File f: files) {

            System.out.println("Try to parse " + f.getName() + " file...");
            CalculateFile cf = new CalculateFile(f);
            isParsed = cf.parseFile();
            if (isParsed) {
                System.out.println("File " + f.getName() + " parsed successfully.");

                res = cf.calculate();
                System.out.println("Result of file " + f.getName() + " with operation " + cf.getOperation() + " = " + res);
                System.out.println();
            }
            else {
                System.out.println("File " + f.getName() + " was not parsed and skipped.");
                System.out.println();
            }

        }
        */
    }


}
