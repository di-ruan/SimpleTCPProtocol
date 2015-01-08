JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	TCPHeader.java \
	senderThread.java \
        sender.java \
        receiverThread.java \
        receiver.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
