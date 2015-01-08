import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Main program for reReceive ACK from receiver
 */

public class sender {

    private String filename;
    private String log_filename;
    private String remote_IP;
    private List<byte[]> msgList;
    private File logFile;
    private BufferedWriter logWriter;
    private senderThread sender;
    private ServerSocket serverSocket;
    private Timer timer;
    private TimerTask timerTask;
    private Date startTime;

    private boolean isFirstLog;
    private boolean isConnected;
    private boolean EOM;
    private boolean isStdout;
    private double alpha = 0.125;
    private double beta = 0.25;
    private double EstimatedRTT;
    private double DevRTT;
    private int remote_port;
    private int ack_port;
    private int window_size;
    private int currentIndex;
    private int currentSeq;
    private int sendBase;
    private int MMS;
    private int TimeOutInterval;
    private int retransmissionTimes;
    private int totalBytes;


    public sender(String filename, String remote_IP, int remote_port, int ack_port, String log_filename, int window_size) {
        this.filename = filename;
        this.remote_IP = remote_IP;
        this.remote_port = remote_port;
        this.ack_port = ack_port;
        this.window_size = 1;
        msgList = new ArrayList<byte[]>();
        isFirstLog = true;
        sender = new senderThread(remote_IP, remote_port, ack_port);
        isConnected = true;
        window_size = 1;
        currentIndex = 0;
        currentSeq = 0;
        EOM = false;
        MMS = 576;
        DevRTT = 0;
        EstimatedRTT = 20;
        sendBase = 0;
        totalBytes = 0;
        retransmissionTimes = 0;
        TimeOutInterval = 1000;

        if (log_filename.equals("stdout")) {
            isStdout = true;
        } else {
            isStdout = false;
            this.log_filename = log_filename;
        }
    }

    public void init() {
        try {
            serverSocket = new ServerSocket(ack_port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readFile() {
        try {
            File file = new File(filename);
            FileInputStream is = new FileInputStream(file);
            BufferedInputStream stream = new BufferedInputStream(is);
            int numByte = stream.available();
            totalBytes = numByte;
            while (numByte > 0) {
                int length = Math.min(numByte, MMS);
                byte[] buffer = new byte[length];
                stream.read(buffer, 0, buffer.length);
                String input = new String(buffer);
                numByte -= buffer.length;
                msgList.add(buffer);
            }
        } catch (Exception e) {
            System.out.println("file not found");
            System.exit(0);
        }
    }

    public void writeToLogFile(String line) {
        try {
            if (isStdout) {
                System.out.println(line);
            } else {
                if (isFirstLog) {
                    logWriter = new BufferedWriter(new FileWriter(log_filename));
                    isFirstLog = false;
                    logWriter.write(line);
                } else {
                    logWriter.newLine();
                    logWriter.write(line);
                }
            }
        } catch (Exception e) {
            System.out.println("file not found");
        }
    }

    public void computeEstRTT(double SampleRTT) {
        EstimatedRTT = (1 - alpha) * EstimatedRTT + alpha * SampleRTT;
    }

    public void computeTimeout(double SampleRTT) {
        DevRTT = (1 - beta) * DevRTT + beta * Math.abs(SampleRTT - EstimatedRTT);
        TimeOutInterval = (int) (EstimatedRTT + 4 * DevRTT);
    }

    public void start() {
        readFile();
        init();
        try {
            sendNextData();
            Socket socket = serverSocket.accept();
            DataInputStream dIn = new DataInputStream(socket.getInputStream());
            while (isConnected) {
                byte[] msg = new byte[20];
                dIn.read(msg);
                TCPHeader header = new TCPHeader();
                header.deserialize(msg);
                if (header.getFIN()) {
                    try {
                        timer.cancel();
                    }
                    catch (Exception e) {}

                    String logMsg = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ", ";
                    logMsg += "source: " + header.getSource() + ", ";
                    logMsg += "destination: " + header.getDestination() + ", ";
                    //logMsg += "Seq #" + header.getSequenceNumber() + ", ";
                    logMsg += "Ack #" + header.getACKNumber() + ", ";
                    logMsg += "ACK, FIN, ";
                    logMsg += "EstimatedRTT: " + String.format("%.2f", EstimatedRTT);
                    writeToLogFile(logMsg);

                    isConnected = false;
                    break;
                } else if (header.getACK()) {

                    String logMsg = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ", ";
                    logMsg += "source: " + header.getSource() + ", ";
                    logMsg += "destination: " + header.getDestination() + ", ";
                    //logMsg += "Seq #" + header.getSequenceNumber() + ", ";
                    logMsg += "Ack #" + header.getACKNumber() + ", ";
                    logMsg += "ACK, ";
                    logMsg += "EstimatedRTT: " + String.format("%.2f", EstimatedRTT);
                    writeToLogFile(logMsg);

                    handleACK(header.getACKNumber());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startTimeOut() {
        try{
            timer = new Timer(true);
            timerTask = new MyTimerTask();
            startTime = Calendar.getInstance().getTime();
            timer.schedule(timerTask, (long) TimeOutInterval);
        }
        catch (Exception e) {}
    }

    public class MyTimerTask extends TimerTask {
        public void run() {
            resend();
        }
    }

    public void sendNextData() {
        int ackNumber = 0;
        short flags = 0;
        short window = 0;

        TCPHeader header = new TCPHeader((short) ack_port, (short) remote_port, currentSeq, ackNumber, flags, window, (short) 0, (short) 0);
        DatagramPacket packet;
        byte[] dataBuffer = msgList.get(currentIndex);
        int len = dataBuffer.length;

        //timestamp, source, destination, Sequence, ACK#, flags
        String logMsg = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ", ";
        logMsg += "source: " + header.getSource() + ", ";
        logMsg += "destination: " + header.getDestination() + ", ";
        logMsg += "Seq #" + header.getSequenceNumber();
        //logMsg += "Ack #" + header.getACKNumber();

        if (EOM) {
            logMsg += ", FIN";
            header.setFIN(true);
            header.setChecksum(new byte[0]);
            packet = new DatagramPacket(header.serialize(), 20);
        } else {
            header.setChecksum(dataBuffer);
            byte[] headerBuffer = header.serialize();
            ByteBuffer buffer = ByteBuffer.allocate(len + 20);
            buffer.put(headerBuffer);
            buffer.put(dataBuffer);
            packet = new DatagramPacket(buffer.array(), len + 20);
            sendBase = currentSeq + len;
        }

        writeToLogFile(logMsg);

        sender.sendData(packet);

        startTimeOut();
    }

    public void resend() {
        if(isConnected) {
            retransmissionTimes++;
            TimeOutInterval = 2*TimeOutInterval;
            //System.out.println("timeout" + TimeOutInterval);
            sendNextData();
        }
    }

    public void handleACK(int ackNumber) {

        if (ackNumber == sendBase) {
            try{
                timer.cancel();
            }
            catch (Exception e){}

            double SampleRTT = Calendar.getInstance().getTime().getTime() - startTime.getTime();
            computeEstRTT(SampleRTT);
            computeTimeout(SampleRTT);
            //System.out.println("New Timeout Interval: " + TimeOutInterval);

            currentSeq = ackNumber;

            if (currentIndex < msgList.size() - 1) {
                currentIndex++;
            }

            if (ackNumber == totalBytes) {
                EOM = true;
            }

            sendNextData();
        }

    }

    public void stop() {
        try {

            String endMsg = "\nDelivery completed successfully\n" + "Total bytes sent = " + totalBytes + "\n"
                    + "Segments sent = " + msgList.size() + "\n" + "Segments retransmitted = " + retransmissionTimes;

            if(!isStdout) {
                System.out.println(endMsg);
            }

            writeToLogFile(endMsg);

            if (logWriter != null) {
                logWriter.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Not enough arguments");
            return;
        }

        if (args.length > 6) {
            System.out.println("Too many arguments");
            return;
        }

        String filename = "";
        String remote_IP = "";
        int remote_port = 0;
        int ack_port = 0;
        String log_filename = "";
        int window_size = 1;

        try {
            filename = args[0];
            remote_IP = args[1];
            remote_port = Integer.parseInt(args[2]);
            ack_port = Integer.parseInt(args[3]);
            log_filename = args[4];
            if (args.length == 6) {
                window_size = Integer.parseInt(args[5]);
            }
            sender snd = new sender(filename, remote_IP, remote_port, ack_port, log_filename, window_size);
            snd.start();
            snd.stop();
        } catch (Exception e) {
            System.out.println("The arguments are not valid");
        }
    }
}
