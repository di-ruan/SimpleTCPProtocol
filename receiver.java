import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Main program for receiver
 */

public class receiver {

    private String filename;
    private String log_filename;
    private String sender_IP;
    private File logFile;
    private File dataFile;
    private BufferedWriter logWriter;
    private BufferedWriter dataWriter;
    private DatagramSocket socket;
    private receiverThread receiver;

    private boolean isFirstLog;
    private boolean isFirstData;
    private boolean isConnected;
    private boolean isStdout;
    private int listen_port;
    private int sender_port;
    private int currentAck;


    public receiver(String filename, int listen_port, String sender_IP, int sender_port, String log_filename) {
        this.filename = filename;
        this.listen_port = listen_port;
        this.sender_IP = sender_IP;
        this.sender_port = sender_port;
        isFirstLog = true;
        isFirstData = true;
        isConnected = false;
        currentAck = 0;
        receiver = new receiverThread(sender_IP, (short)sender_port, (short)listen_port);

        if (log_filename.equals("stdout")) {
            isStdout = true;
        } else {
            isStdout = false;
            this.log_filename = log_filename;
        }
    }

    public void writeToDataFile(String line) {
        try {
            if (isFirstData) {
                dataWriter = new BufferedWriter(new FileWriter(filename));
                isFirstData = false;
                dataWriter.write(line);
            } else {
                dataWriter.write(line);
            }
        } catch (Exception e) {
            System.out.println("unable to create file");
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
            System.out.println("unable to create file");
        }
    }

    public void start() {
        try {
            socket = new DatagramSocket(listen_port);
            byte arr1[] = new byte[1024];
            DatagramPacket packet = new DatagramPacket(arr1, arr1.length);
            isConnected = true;
            while (isConnected) {
                socket.receive(packet);
                int length = packet.getLength();
                byte[] buffer = new byte[length];
                byte[] header = new byte[20];
                byte[] data = new byte[length - 20];

                buffer = packet.getData();

                System.arraycopy(buffer, 0, header, 0, 20);
                System.arraycopy(buffer, 20, data, 0, length - 20);

                TCPHeader myHeader = new TCPHeader();
                myHeader.deserialize(header);

                if (!myHeader.isCorrupted(data)) {
                    String logMsg = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ", ";
                    logMsg += "source: " + myHeader.getSource() + ", ";
                    logMsg += "destination: " + myHeader.getDestination() + ", ";
                    logMsg += "Seq #" + myHeader.getSequenceNumber();
                    writeToLogFile(logMsg);

                    if (myHeader.getFIN()) {
                        logMsg = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ", ";
                        logMsg += "source: " + listen_port + ", ";
                        logMsg += "destination: " + sender_port + ", ";
                        logMsg += "Ack #" + currentAck + ", ";
                        logMsg += "ACK, FIN";
                        writeToLogFile(logMsg);
                        receiver.sendFIN(currentAck);
                        Thread.sleep(10);
                        break;
                    } else {
                        int seqNum = myHeader.getSequenceNumber();
                        if (currentAck == seqNum) {
                            currentAck = seqNum + data.length;
                            writeToDataFile(new String(data, "UTF-8"));

                            logMsg = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ", ";
                            logMsg += "source: " + listen_port + ", ";
                            logMsg += "destination: " + sender_port + ", ";
                            logMsg += "Ack #" + currentAck + ", ";
                            logMsg += "ACK";
                            writeToLogFile(logMsg);

                            receiver.sendACK(currentAck);
                        }
                    }
                } else {
                    //System.out.println("package is corrupted");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            System.out.println("\nReception completed successfully\n");
            if (logWriter != null) {
                logWriter.close();
            }
            if (dataWriter != null) {
                dataWriter.close();
            }
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Not enough arguments");
            return;
        }

        if (args.length > 5) {
            System.out.println("Too many arguments");
            return;
        }

        String filename = "";
        int listen_port = 0;
        String sender_IP = "";
        int sender_port = 0;
        String log_filename = "";

        try {
            filename = args[0];
            listen_port = Integer.parseInt(args[1]);
            sender_IP = args[2];
            sender_port = Integer.parseInt(args[3]);
            log_filename = args[4];
            receiver rcv = new receiver(filename, listen_port, sender_IP, sender_port, log_filename);
            rcv.start();
            rcv.stop();
        } catch (Exception e) {
            System.out.println("The arguments are not valid");
        }
    }
}
