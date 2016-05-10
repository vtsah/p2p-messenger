
JFLAGS = -g -Xlint
JC = javac

all:
	$(JC) $(JFLAGS) *.java

docs:
	javadoc -d docs *.java

clean:
	rm -f *.class
