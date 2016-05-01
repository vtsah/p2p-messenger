
JFLAGS = -g
JC = javac

all:
	$(JC) $(JFLAGS) *.java

clean:
	rm -f *.class
