import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CalculateFile implements Runnable{
    //Link to current thread
    private Thread thread;

    private static ArrayList<Thread> threads = new ArrayList<>(); //ArrayList with all started threads to join them into Main thread
    private static ConcurrentLinkedQueue<File> filesQueue = new ConcurrentLinkedQueue<>(); //queue with all files to process
    private static int cntProcessed = 0; //General number of processed files by all threads
    private int cntProcessedThread; //Number of processed files by each thread

    static EnumLogModes logMode; //If thue - print logging information
    static File resultFile; //Link to the Results file
    static Map<File, String> hashmap; //Map to store information about which thread was processed each file

    private File file; //Link to current file from queue
    private FileReader fin = null;

    private int operation;
    private double[] operands;
    private double result;
    private boolean isParsed = false;

    CalculateFile() {
        this.thread = new Thread(this);
        threads.add(thread);
        thread.start();
    }

    private static void doLog(String msg, EnumLogStatus st) {
        switch (logMode) {
            case HIGH:
                System.out.println("[" + st.toString() + "] " + "[Thread " + Thread.currentThread().getId() + "] " + msg);
                break;
            case MED:
                if (st.equals(EnumLogStatus.MUSTSEE)) {
                    System.out.println("[Thread " + Thread.currentThread().getId() + "] " + msg);
                }
                if (st.equals(EnumLogStatus.WARNING) || st.equals(EnumLogStatus.ERROR)) {
                    System.out.println("[" + st.toString() + "] " + "[Thread " + Thread.currentThread().getId() + "] " + msg);
                }
                break;
            case LOW:
                if (st.equals(EnumLogStatus.MUSTSEE)) {
                    System.out.println("[Thread " + Thread.currentThread().getId() + "] " + msg);
                }
                if (st.equals(EnumLogStatus.ERROR)) {
                    System.out.println("[" + st.toString() + "] " + "[Thread " + Thread.currentThread().getId() + "] " + msg);
                }
                break;
            case OFF:
                if (st.equals(EnumLogStatus.MUSTSEE)) {
                    System.out.println("[Thread " + Thread.currentThread().getId() + "] " + msg);
                }
                break;
        }
    }

    @Override
    public void run () {
        doLog("started.", EnumLogStatus.INFO);

        startProcess();

        doLog("terminating.", EnumLogStatus.INFO);
        doLog("calculated " + cntProcessedThread + " files.", EnumLogStatus.MUSTSEE);
    }

    private void startProcess() {

        while (!filesQueue.isEmpty()) {
            file = getFileFromQueue(); //filesQueue.poll();
            if (file == null) {
                doLog("Queue is empty", EnumLogStatus.WARNING);
                return;
            }

            markMap(file,thread);

            doLog("Got file " + file.getName() + " to process", EnumLogStatus.INFO);

            if (parseFile()) {
                result = calculate();
                doLog("File " + file.getName() + " processed. Result = " + result, EnumLogStatus.INFO);
                writeResult(result);
            } else {
                doLog("File " + file.getName() + " was not been parsed and skipped.", EnumLogStatus.WARNING);
            }
            setCntProcessed();
            cntProcessedThread ++;
        }
    }

    public static int getCntProcessed() {
        return cntProcessed;
    }

    private static synchronized void setCntProcessed() {
        CalculateFile.cntProcessed ++;
    }

    public static ArrayList<Thread> getThreads() {
        return threads;
    }

    private static synchronized void markMap(File f, Thread th) {
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
        double writeResult;

        if (!resultFile.exists() || resultFile == null) {
            doLog("Results file does not exists", EnumLogStatus.ERROR);
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(resultFile, "rw")) {
            raf.seek(0);
            currResult = raf.readDouble();

            doLog("Current result from file = " + currResult, EnumLogStatus.INFO);

            raf.seek(0);
            writeResult = currResult + addResult;
            raf.writeDouble(writeResult);

            raf.seek(0);
            doLog("New result written to file = " + writeResult, EnumLogStatus.INFO);
        }
        catch (FileNotFoundException e) {
            doLog("File " + resultFile.getName() + " not found.", EnumLogStatus.ERROR);
        }
        catch (IOException e) {
            doLog("File " + resultFile.getName() + ". IOException", EnumLogStatus.ERROR);
        }
    }

    private boolean parseFile() {
        isParsed = false;

        try {
            fin = new FileReader(file.getAbsolutePath());

            try (BufferedReader br = new BufferedReader(fin)) {
                String lineOne, lineTwo;

                if ((lineOne = br.readLine()) != null) {
                    try {
                        operation = Integer.parseInt(lineOne);

                        if (operation != 1 && operation != 2 && operation !=3 ) {
                            doLog("File " + file.getName() + " wrong:\n" +
                                    "Line 1 cannot be converted to operation. Possible values:\n" +
                                    "1 - addition\n" +
                                    "2 - multiplication\n" +
                                    "3 - sum of quaters",
                                    EnumLogStatus.WARNING);
                            return isParsed;
                        }
                    }
                    catch (NumberFormatException e) {
                        doLog("File " + file.getName() + " wrong:\n" +
                                        "Line 1 cannot be converted to operation.",
                                EnumLogStatus.WARNING);
                        return isParsed;
                    }
                }
                else {
                    doLog("File " + file.getName() + " is empty", EnumLogStatus.WARNING);
                    return isParsed;
                }

                if ((lineTwo = br.readLine()) != null) {
                    try {
                        String[] op = lineTwo.split(" ");
                        operands = new double[op.length];

                        int i=0;
                        for (String s: op) {
                            operands[i++] = Double.parseDouble(s);
                        }
                    }
                    catch (NumberFormatException e) {
                        doLog("File " + file.getName() + " wrong:\n" +
                                        "Line two cannon be converted to double-values",
                                EnumLogStatus.WARNING);
                        return isParsed;
                    }
                }
                else {
                    doLog("File " + file.getName() + " wrong:\n" +
                                    "Missed the second line with operands",
                            EnumLogStatus.WARNING);
                    return isParsed;
                }

            }
            catch (IOException e) {
                doLog("File reading error " + file.getName(), EnumLogStatus.ERROR);
                return isParsed;
            }

        }
        catch (FileNotFoundException e) {
            doLog("File "+ file.getAbsolutePath() + " does not exist", EnumLogStatus.ERROR);
            return isParsed;
        }
        finally {
            try {
                fin.close();
            }
            catch (IOException e) {
                doLog("File closing error " + file.getAbsolutePath(), EnumLogStatus.ERROR);
            }
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
            doLog("First you should parse the file!", EnumLogStatus.ERROR);
            return result;
        }

        return result;
    }

}
