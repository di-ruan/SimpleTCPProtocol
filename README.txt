
(1) How to invoke the program

>make

>./newudpl -o localhost:20000 -i localhost:20001 -p 5000:6000 -L 20 -B 20 -O 20 -d 0.01

>java receiver file.txt 20000 127.0.0.1 20001 rcv_log.txt

>java sender input.txt 127.0.0.1 5000 20001 snd_log.txt 1




(2) Program features

1. The sender can send a txt file to receiver
2. This program only support window size = 1
3. The maximal segment size is 576
4. Both receiver and sender will log the header information of the uncorrupted packet
5. The checksum is computed over the header and the data
6. In the statistics, we show the total bytes which have been sent successfully, the
   total number of segments sent and the retransmission times.






(3) Description

(a) TCP segment structure used

The TCP segment contains 20 bytes of header and maximal 576 bytes of data.
In the header, it contains:
2 bytes of source port;
2 bytes of destination port;
4 bytes of sequence number;
4 bytes of acknowledge number;
2 bytes of unused and some flags;
2 bytes of receive window;
2 bytes of checksum;
2 bytes of urgent data pointer;


(b) The states typically visited by sender and receiver

For sender:
It has three states,
1) send a packet and wait
2) receive ack and send the next
3) timeout, resend the packet

For receiver:
It has two states,
1) receive a correct packet and send ACK
2) receive a corrupted packet and ignore it


(c) The loss recovery mechanism

Since the window size is 1, for sender, once the packet is sent and we
start a timer. If we receive an ACK from the receiver within the timeout
interval, we cancel the timer. Otherwise, we resend the packet. For the
receiver, we wait for an uncorrupted packet, if the sequence number is
what we asked for, then we send ACK to the sender.

If the data loss or corruption happens, the timeout mechanism will make
sure it is sent again from the sender.





(4) Additional feature

The timeout interval is sent to 1 second at the beginning. When a timeout
occurs, it is doubled to avoid a premature timeout. As soon as a segment
is received, it will be updated by computing the EstimatedRTT and DevRTT.
