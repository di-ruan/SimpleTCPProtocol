import java.net.*;
import java.util.Calendar;
import java.util.Date;

/**
 * Second thread for sender who sends packet to receiver
 */
public class senderThread extends Thread {

    private int remote_port;
    private String remote_IP;
    private DatagramSocket socket;
    private SocketAddress address;

    public senderThread(String remote_IP, int remote_port, int ack_port) {
        try {
            this.remote_IP = remote_IP;
            this.remote_port = remote_port;
            socket = new DatagramSocket(ack_port);
        } catch (Exception e) {
            System.out.println("create thread fail");
        }
    }

    public void sendData(DatagramPacket packet) {
        try {
            packet.setAddress(InetAddress.getByName(remote_IP));
            packet.setPort(remote_port);

            if (socket != null) {
                socket.send(packet);
            }
        } catch (Exception e) {
            System.out.println("send fail");
            e.printStackTrace();
        }
    }
}
