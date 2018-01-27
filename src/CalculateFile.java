import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CalculateFile implements Runnable{
    Thread thread;

    private static ConcurrentLinkedQueue<File> filesQueue = new ConcurrentLinkedQueue<>();
    private static ArrayList<Thread> threads = new ArrayList<>();
    private static int cntProcessed = 0;
    private static boolean debugMode;
    private int cntProcessedThread ;
    private static String resultFilePath;

    private static Map<File, String> hashmap;

    private File file;
    FileReader fin = null;

    private int operation;
    private double[] operands;
    private double result;
    private boolean isParsed = false;

    public CalculateFile(boolean debugMode, Map<File, String> hashmap, String resultFilePath) {
        this.thread = new Thread(this);
        this.debugMode = debugMode;
        threads.add(thread);
        thread.start();
        this.hashmap = hashmap;
        this.resultFilePath = resultFilePath;
    }

    @Override
    public void run () {
        System.out.println("[Thread " + thread.getId() + "] started.");
        startProcess();
        System.out.println("[Thread " + thread.getId() + "] terminating.");
        System.out.println("[Thread " + thread.getId() + "] calculate " + cntProcessedThread + " files.");
    }

    private void startProcess() {

        while (!filesQueue.isEmpty()) {
            file = getFileFromQueue(); //filesQueue.poll();
            if (file == null) {
                System.out.println("Queue is empty");
                return;
            }

            markMap(file,thread);

            if (debugMode) {System.out.println("[Thread " + thread.getId() + "] got file " + file.getName() + " to process");}

            if (parseFile()) {
                result = calculate();
                if (debugMode) {System.out.println("[Thread " + thread.getId() + "] File " + file.getName() + " processed. Result = " + result);}
                writeResult(result);
            } else {
                if (debugMode) {System.out.println("[Thread " + thread.getId() + "] File " + file.getName() + " was not been parsed and skipped.");}
            }
            /*
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                System.out.println("Error sleeping");
            }
            */
            setCntProcessed();
            cntProcessedThread ++;
        }

    }

    public static int getCntProcessed() {
        return cntProcessed;
    }

    public static synchronized void setCntProcessed() {
        CalculateFile.cntProcessed ++;
    }

    public static ArrayList<Thread> getThreads() {
        return threads;
    }

    public static synchronized void markMap(File f, Thread th) {
        hashmap.put(f, hashmap.get(f) + "," + th.getId());
    }

    public static void addFileToQueue(File file) {
        filesQueue.add(file);
    }

    private synchronized File getFileFromQueue() {
        return filesQueue.poll();
    }

    private String getOperation() {
        return operation == 1 ? "Addition" : operation == 2 ? "Multiplication" : operation == 3 ? "Sum of quaters" : "";
    }



    private static synchronized void writeResult(double addResult) {
        double currResult;
        boolean isDeleted;

        File fileObject = new File(resultFilePath);
        if (fileObject.exists()) {
            isDeleted = fileObject.delete();
            System.out.println("deleted="+isDeleted);
            try {
                fileObject.createNewFile();
            }
            catch (IOException e) {
                System.out.println("File wasn't created!" );
                e.printStackTrace();
                return;
            }
        }

        try (RandomAccessFile raf = new RandomAccessFile(resultFilePath, "rw")) {
            raf.seek(0);
            raf.writeUTF("");
            //raf.writeUTF("0.0");
            //raf.seek(0);
/*
            //currResult = raf.readDouble();
            currResult = Double.parseDouble(raf.readUTF());
            raf.seek(0);
            //raf.writeDouble(currResult + addResult);
            raf.writeUTF(Double.toString(currResult+addResult));
            raf.seek(0);*/
        }
        catch (FileNotFoundException e) {
            System.out.println("Exception! File " + resultFilePath + " not found.");
        }
        catch (IOException e) {
            System.out.println("Exception! File " + resultFilePath + ". IOException");
        }
/*
        try (BufferedWriter bf = new BufferedWriter(new FileWriter(new File(filePath)))) {
            bf.write(str);
            bf.newLine();
        } catch (java.io.IOException e) {
            System.out.println("Error writing a result");
        }
        */
    }

    private boolean parseFile() {
        isParsed = false;

        try {
            fin = new FileReader(file.getAbsolutePath());

            try (BufferedReader br = new BufferedReader(fin)) {
                String lineOne, lineTwo;

                if ((lineOne = br.readLine()) != null) {
                    //System.out.println("1 line = " + lineOne);
                    try {
                        operation = Integer.parseInt(lineOne);

                        if (operation != 1 && operation != 2 && operation !=3 ) {
                            System.out.println("File " + file.getName() + " Exception!");
                            System.out.println("Line 1 cannot be converted to operation. Possible values:");
                            System.out.println("1 - addition");
                            System.out.println("2 - multiplication");
                            System.out.println("3 - sum of quaters");
                            return isParsed;
                        }
                    }
                    catch (NumberFormatException e) {
                        System.out.println("File " + file.getName() + " Exception!");
                        System.out.println("Line 1 cannot be converted to operation.");
                        return isParsed;
                    }
                }
                else {
                    System.out.println("File " + file.getName() + " is missed the first line with code operation");
                    return isParsed;
                }

                if ((lineTwo = br.readLine()) != null) {
                    //System.out.println("2 line = " + lineTwo);

                    try {
                        String[] op = lineTwo.split(" ");
                        operands = new double[op.length];

                        int i=0;
                        for (String s: op) {
                            //System.out.println("Try to convert ="+s);
                            operands[i++] = Double.parseDouble(s);
                        }
                    }
                    catch (NumberFormatException e) {
                        System.out.println("Line two cannon be converted to double-values");
                        return isParsed;
                    }
                }
                else {
                    System.out.println("File " + file.getName() + " is missed the second line with operands");
                    return isParsed;
                }

            }
            catch (IOException e) {
                e.printStackTrace();
                return isParsed;
            }

        }
        catch (FileNotFoundException e) {
            System.out.println("File "+ file.getAbsolutePath() + " does not exist");
            return isParsed;
        }

        isParsed = true;
        return isParsed;
    }

    private double calculate() {
        result = 0.0d;

        if (isParsed) {
            switch (operation) {
                case 1:
                    for (double op: operands) {
                        result+=op;
                    }
                    break;
                case 2:
                    result = 1.0d;
                    for (double op: operands) {
                        result*=op;
                    }
                    break;
                case 3:
                    for (double op: operands) {
                        result += Math.pow(op,2);
                    }
                    break;
            }
        }
        else {
            System.out.println("First you should parse the file!");
            return result;
        }

        return result;
    }

}
