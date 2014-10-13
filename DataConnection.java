import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;


public class DataConnection {

	private Socket dataSocket;
	private Thread readerThread;
	private ServerDataReader serverReader;
	private ServerFileWriter serverFileWriter;
	private CmdConnection cmdConnection;
	private String[] currentCommands;
	private boolean readyForFile;
	private boolean binaryMode;
	private static final int TIMEOUT_LIMIT = 30000; // 30 seconds

	public DataConnection(CmdConnection cmdConnection) {
		dataSocket = null;
		serverReader = null;
		serverFileWriter = null;
		this.cmdConnection = cmdConnection;
	}
	/*
	 * Connects a socket to the server at the given ip and port to create the
	 * data connection for transferring files.
	 */
	public boolean connect(String ip, int port) {

		try {
			dataSocket = new Socket();
			InetSocketAddress iNet = new InetSocketAddress(ip, port);
			dataSocket.connect(iNet, TIMEOUT_LIMIT);

			serverReader = new ServerDataReader(dataSocket, cmdConnection, this);
			serverReader.setCommandString(currentCommands);

			return true;

		} catch (UnknownHostException e) {
			System.out.println("930 Data transfer connection to " + ip
					+ " on port " + port + " failed to open");
			close();
			return false;

		} catch (SocketTimeoutException e) {
			System.out.println("930 Data transfer connection to " + ip
					+ " on port " + port + " failed to open");
			close();
			return false;
		} catch (IOException e) {
			System.out.println("935 Data transfer connection I/O error, "
					+ "closing control connection.");
			close();
			return false;
		}
	}
	
	/*
	 * Configures the ServerDataReader to receive a file and spawns a new thread
	 * to receive the file.
	 * parameters: FileOutputStream outStream, String[] command
	 */
	public void receiveFile(FileOutputStream outStream, String[] command) {
		serverReader.setOutStream(outStream);
		serverReader.setCommandString(command);

		// Spawn new thread to read data from server.
		readerThread = new Thread(serverReader);
		readerThread.start();
	}

	/*
	 * Configures the ServerDataReader to receive a directory and spawns a new thread
	 * to receive the directory. Command indicates whether the reader is looking for a 
	 * directory or a file. This thread is then joined to ensure uninterrupted printing
	 * of the directory in the Standard out. 
	 * parameter: String[] command
	 */
	public void receiveDir(String[] command) {
		serverReader.setCommandString(command);
		readerThread = new Thread(serverReader);

		// Spawn new thread to read data from server.
		readerThread.start();
		try {
			readerThread.join();
		} catch (InterruptedException e) {
			System.out.println("999 Processing error. The directory read could "
					+ "not be completed.");
		}
	}

	/*
	 * Configures the ServerFileWriter to send a file and spawns a new thread
	 * to send the file. 
	 * parameters: FileInputStream fileIn
	 */
	public void sendFile(FileInputStream fileIn)  {
		serverFileWriter = new ServerFileWriter(dataSocket, this);
		serverFileWriter.setFileInStream(fileIn);

		// Spawn new thread to write file to server.
		Thread t;
		t = new Thread(serverFileWriter);
		t.start();
	}

	/* 
	 * Closes the data socket and clears the ServerDataReader
	 */
	public void close() {
		try {
			if (dataSocket != null) {
				dataSocket.close();
				dataSocket = null;	
				
			}
			serverReader = null;
		} catch (Exception e) {
			System.out.println(" 999 Processing error. "
					+ "An error occurred while closing the data connection.");
		}
		
	}

	public void setServerReadyForFile(boolean ready) {	
		readyForFile = ready;
	}

	public void setIsServerBinaryMode(boolean binary) {		
		binaryMode = binary;
	}
}
