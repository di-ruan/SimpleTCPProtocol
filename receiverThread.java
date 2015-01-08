import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Second thread for receiver who sends Ack or FIN to sender
 */

public class receiverThread extends Thread {
    private Socket clientSocket;
    private DataOutputStream dOut;
    private String IP;
    private short port;
    private short sourcePort;
    private boolean isFirstACK;

    public receiverThread(String IP, short port, short sourcePort) {
        this.IP = IP;
        this.port = port;
        this.sourcePort = sourcePort;
        isFirstACK = true;
    }

    public void sendACK(int ackNum) {
        try {
            if (isFirstACK && clientSocket == null) {
                clientSocket = new Socket(IP, port);
                dOut = new DataOutputStream(clientSocket.getOutputStream());
                isFirstACK = false;
            }

            TCPHeader myHeader = new TCPHeader();
            myHeader.setSource(sourcePort);
            myHeader.setDestination(port);
            myHeader.setACK(true);
            myHeader.setACKNumber(ackNum);

            dOut.write(myHeader.serialize());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFIN(int ackNum) {
        try {
            if (isFirstACK && clientSocket == null) {
                clientSocket = new Socket(IP, port);
                dOut = new DataOutputStream(clientSocket.getOutputStream());
                isFirstACK = false;
            }

            TCPHeader myHeader = new TCPHeader();
            myHeader.setSource(sourcePort);
            myHeader.setDestination(port);
            myHeader.setACKNumber(ackNum);
            myHeader.setFIN(true);

            dOut.write(myHeader.serialize());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
