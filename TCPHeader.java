import java.nio.ByteBuffer;

/**
 * The TCPHeader class takes care of serialization and deserialization of header and computing checksum,
 */

public class TCPHeader {
    private final byte Flag_URG = 0x20;
    private final byte Flag_ACK = 0x10;
    private final byte Flag_PSH = 0x08;
    private final byte Flag_RST = 0x04;
    private final byte Flag_SYN = 0x02;
    private final byte Flag_FIN = 0x01;

    private short sourcePort;
    private short destinationPort;
    private int seqNumber;
    private int ackNumber;
    private short flags;
    private short window;
    private short checksum;
    private short urgentPointer;

    public TCPHeader() {

    }

    public TCPHeader(short srcPort, short dstPort, int seqNumber, int ackNumber,
                     short flags, short window, short checksum, short urgentPointer) {
        this.sourcePort = srcPort;
        this.destinationPort = dstPort;
        this.seqNumber = seqNumber;
        this.ackNumber = ackNumber;
        this.flags = flags;
        this.window = window;
        this.checksum = checksum;
        this.urgentPointer = urgentPointer;
    }

    public void deserialize(byte[] headerBytes) {
        ByteBuffer buffer = ByteBuffer.wrap(headerBytes);
        sourcePort = buffer.getShort(0);
        destinationPort = buffer.getShort(2);
        seqNumber = buffer.getInt(4);
        ackNumber = buffer.getInt(8);
        flags = buffer.getShort(12);
        window = buffer.getShort(14);
        checksum = buffer.getShort(16);
        urgentPointer = buffer.getShort(18);
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.putShort(0, sourcePort);
        buffer.putShort(2, destinationPort);
        buffer.putInt(4, seqNumber);
        buffer.putInt(8, ackNumber);
        buffer.putShort(12, flags);
        buffer.putShort(14, window);
        buffer.putShort(16, checksum);
        buffer.putShort(18, urgentPointer);
        return buffer.array();
    }

    public short computeChecksum(byte[] data) {
        int sum = 0;
        int length = 20 + data.length + data.length % 2;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.put(serialize());
        buf.put(data);
        buf.putShort(16, (short) 0);         //set checksum field to 0
        byte[] buffer = buf.array();

        for (int i = 0; i < length; i = i + 2) {
            int word16 = ((buffer[i] << 8) & 0xFF00) + (buffer[i + 1] & 0xFF);
            sum += word16;
        }

        sum = (sum >>> 16) + (sum & 0xffff);
        sum += sum >>> 16;

        return (short) ~sum;
    }

    public short getSource() {
        return sourcePort;
    }

    public void setSource(short srcPort) {
        sourcePort = srcPort;
    }

    public short getDestination() {
        return destinationPort;
    }

    public void setDestination(short dstPort) {
        destinationPort = dstPort;
    }

    public void setChecksum(byte[] data) {
        checksum = computeChecksum(data);
    }

    public boolean isCorrupted(byte[] data) {
        return checksum != computeChecksum(data);
    }

    public void setACK(boolean isACK) {
        if (isACK) {
            flags = (short) (flags | Flag_ACK);
        } else {
            flags = (short) (flags & ~Flag_ACK);
        }
    }

    public boolean getACK() {
        return (flags & Flag_ACK) == Flag_ACK;
    }

    public void setFIN(boolean isFIN) {
        if (isFIN) {
            flags = (short) (flags | Flag_FIN);
        } else {
            flags = (short) (flags & ~Flag_FIN);
        }
    }

    public boolean getFIN() {
        return (flags & Flag_FIN) == Flag_FIN;
    }

    public void setSequenceNumber(int seqNum) {
        seqNumber = seqNum;
    }

    public int getSequenceNumber() {
        return seqNumber;
    }

    public void setACKNumber(int ackNum) {
        ackNumber = ackNum;
    }

    public int getACKNumber() {
        return ackNumber;
    }
}
