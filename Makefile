
JFLAGS = -g -Xlint
JC = javac

all:
	$(JC) $(JFLAGS) *.java

docs:
	javadoc -d documentation *.java

clean:
	rm -f *.class
