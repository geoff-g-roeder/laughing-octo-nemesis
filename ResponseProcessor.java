

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ResponseProcessor {

	private Socket socket;
	private CmdConnection cmdConnection;

	BufferedReader socketReader;
	private String receivedMessage = "";

	public ResponseProcessor(Socket socket, CmdConnection cmdConnection) {
		this.socket = socket;
		this.cmdConnection = cmdConnection;
		try {
			socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			System.out.println("925 Control connection I/O error, closing control connection.");
			cmdConnection.close();
		}
	}

	/*
	 * Listens for the server's responses and prints them to System.out. Returns the final
	 * response of the server.
	 */
	public synchronized String listen()  {
		try {
			while (!socket.isClosed()) {
				while ((receivedMessage = socketReader.readLine()) != null) {		
					System.out.println("<-- " + receivedMessage);

						if (isDone(receivedMessage)) {
							return receivedMessage;
						}					
				}
			}
		} catch (IOException e) {
			System.out.println("925 Control connection I/O error, closing control connection.");
			cmdConnection.close();
		} 
		return null;
	}
	
	public synchronized String listenDone()  {
		try {
			while (!socket.isClosed()) {
				while ((receivedMessage = socketReader.readLine()) != null) {	
						if (isDone(receivedMessage)) {
							return receivedMessage;
						}					
				}
			}
		} catch (IOException e) {
			System.out.println("925 Control connection I/O error, closing control connection.");
			cmdConnection.close();
		} 
		return null;
	}

	// Listens for PASV related responses from the server
	public String listenPASV() {

		String response = listen();
		if (response != null)  {
			if (isPositiveCompletion(response)) {
				return response;
			}
		}
		return null;
	}

	public boolean listenDir() {
		return listenPositivePreliminary();
	}

	public boolean listenStor() {
		return listenPositivePreliminary();
	}

	public boolean listenGet() {
		return listenPositivePreliminary();
	}
	
	// Listens for positive responses from the server, returns true if response is positive
	private synchronized boolean listenPositivePreliminary() {

		String response = listen();

		if (response != null) {
			return isPositivePreliminary(response);
		}
		return false;
	}

	// Listens for CWD related responses from the server
	public boolean listenCWD() {
		String response = listen();

		if (response != null) {
			// Server switching to Binary mode
			if (isPositiveCompletion(receivedMessage)) { 
				return true;
			} 
		}
		return false;
	}
	
	// Listens for password related responses from the server
	public synchronized String listenPw() {

		String response = listen();
		String seenCode = "";

		if (response != null)  {
			if (receivedMessage.startsWith("331 ")) {
				seenCode = "331";
				return seenCode;
			} else if (receivedMessage.startsWith("332 ")) {
				seenCode = "332";
				return seenCode;
			} else if (receivedMessage.startsWith("230 ")) {
				seenCode =  "230";
				return seenCode;
			} 		
		}
		return seenCode;
	}
	
	private boolean isDone(String receivedMessage) {
		return receivedMessage.matches("\\d\\d\\d\\s.*");
	}

	private boolean isPositivePreliminary(String message) {
		return receivedMessage.matches("1\\d\\d\\s.*");
	}

	// Sent when action has been successfully completed by server 
	private boolean isPositiveCompletion(String message) {
		return receivedMessage.matches("2\\d\\d\\s.*");
	}

	/*
	 *  The command has been accepted, but the requested action
               is being held in abeyance, pending receipt of further
               information.  The user should send another command
               specifying this information.  This reply is used in
               command sequence groups.
               
               Left in for future expansion to other commands.
	 */
	private boolean isPositiveIntermediate(String message) {
		return receivedMessage.matches("3\\d\\d\\s.*");
	}

	/*
	 * 
	 * The command was not accepted and the requested action did
               not take place, but the error condition is temporary and
               the action may be requested again.  The user should
               return to the beginning of the command sequence, if any.

               The command was not accepted and the requested action did
               not take place.  The User-process is discouraged from
               repeating the exact request (in the same sequence).  
               
               Left in for future expansion to other commands.
	 */
	private boolean isNegativeTransOrPerm(String message) {
		return receivedMessage.matches("4\\d\\d\\s.*")
				|| receivedMessage.matches("5\\d\\d\\s.*");
	}

	public synchronized boolean listenBin() {

		String response = listen();

		if (response != null) {
			if (receivedMessage.startsWith("200 ")) { // Server switching to Binary mode
				// Binary mode entered
				return true;
			} 
		}
		return false;
	}
}
