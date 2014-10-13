all: CSftp.jar

CLASSFILES=CSftp.class InputProcessor.class CmdConnection.class CommandWriter.class DataConnection.class ResponseProcessor.class ServerDataReader.class ServerFileWriter.class

CSftp.class: CSftp.java
	javac CSftp.java
InputProcessor.class: InputProcessor.java
	javac InputProcessor.java
CmdConnection.class: CmdConnection.java
	javac CmdConnection.java
CommandWriter.class: CommandWriter.java
	javac CommandWriter.java
DataConnection.class: DataConnection.java	
	javac DataConnection.java
ResponseProcessor.class: ResponseProcessor.java	
	javac ResponseProcessor.java
ServerDataReader.class: ServerDataReader.java	
	javac ServerDataReader.java
ServerFileWriter.class: ServerFileWriter.java	
	javac ServerFileWriter.java
	jar cvfe CSftp.jar CSftp *.class

CSftp.jar: $(CLASSFILES)
	jar cvfe CSftp.jar CSftp $(CLASSFILES)



run: CSftp.jar
	java -jar CSftp.jar

clean:
	rm -f *.class
	rm -f CSftp.jar
