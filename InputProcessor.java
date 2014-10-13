
import java.lang.System;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;

public class InputProcessor {

	String[] command;
	CmdConnection cmdConnection;

	DataConnection dataConnection; 

	public InputProcessor() {
		command = null;
		cmdConnection = null;
		dataConnection = null;
	}

	public boolean processCommand(byte[] byteArrayCmd) {
		String input = byteArrayToString(byteArrayCmd);
		parseCommand(input);

		// Have to clear the byteArray for looping or the previous
		// command sticks around.
		Arrays.fill(byteArrayCmd, (byte) 0);		


		if (command[0].toLowerCase().equals("quit")) {
			return true;
		} else if (command[0].equals("<CR>") || command[0].startsWith("#")){
			// Repeat prompt in case of carriage return or comment line starting with #
			return false;
		} 

		//=========================================================================================
		// Start command connection processing 
		else if (command[0].toLowerCase().equals("open")) {

			if (isCmdConnOpen()) {
				System.out.println("903 Supplied command not"
						+ " expected at this time."); 
				return false;
			}

			if (command.length > 1 && command.length < 4) {
				// Case 1: port number provided. Validate first.
				if (command.length == 3) {
					if (isValidPort(command[2])) {
						processOpen();
					} else { 
						System.out.println("902 Invalid argument.");
						return false;
					}
				} else {
					// Case 2: no port number provided. Use default.
					processOpen();
				}
			} else {
				System.out.println("901 Incorrect number of arguments.");
				return false;
			}

		} else if (command[0].toLowerCase().equals("user")) {
			if (!isCmdConnOpen() || cmdConnection.isUserLoggedIn()) {
				System.out.println("903 Supplied command not"
						+ " expected at this time."); 
				return false;
			}
			if (command.length > 1 && command.length < 3) {
				processUser(command[1]);
			} else {
				System.out.println("901 Incorrect number of arguments.");
				return false;
			}

		} else if (command[0].toLowerCase().equals("close")) {
			if (!isCmdConnOpen()) {
				System.out.println("903 Supplied command not"
						+ " expected at this time."); 
			} else {
				closeConnections();
			} 		
		}
		
		//========================================================================
		// Start Data connection command processing
		else if (isValidDataCommand(command[0])) {
			if (!isCmdConnOpen() || !cmdConnection.isUserLoggedIn()) {
				System.out.println("903 Supplied command not"
						+ " expected at this time."); 
				return false;
				
			} else {

				if (command[0].toLowerCase().equals("cd")) {
					processCd();
				} else if (command[0].toLowerCase().equals("get") || 
						command[0].toLowerCase().equals("put")) {
					processFileCmd();

				} else if (command[0].toLowerCase().equals("dir")) {
					processDir();
				} 
			} 
		} else {
			System.out.println("900 Invalid command.");
		}
		return false;
	}

	private boolean isValidPort(String port) {	
		return 1 <= Integer.valueOf(port) && Integer.valueOf(port)<= 65535;
	}

	private void processCd() {
		if (command.length > 1 && command.length < 3) {		
			cmdConnection.sendCd(command[1]);
		} else {
			System.out.println("901 Incorrect number of arguments");
		}
	}

	private void processDir() {
		if (command.length == 1) {
			if (cmdConnection.requestDataConn()) {
				if (cmdConnection.sendList()) {
					String response = cmdConnection.getResponseProcessor().listenDone();
					dataConnection.receiveDir(command);
					System.out.println("<-- " + response);
				}
			}
		} else {
			System.err.println("901 Incorrect number of arguments");
		}
	}

	private void processFileCmd() {
		if (command.length > 2 && command.length < 4) {
			if (cmdConnection.requestDataConn()) { 
				if (cmdConnection.requestBin()) {			
					if (command[0].toLowerCase().equals("get")) {
						processGet();
					} else if (command[0].toLowerCase().equals("put")) {
						processPut();
					}
				} else {
					System.out.println("999 Processing error. "
							+ "Server did not acknowledge binary mode.");
				}
			}
			dataConnection.setIsServerBinaryMode(false);
			dataConnection.setServerReadyForFile(false);
		} else {
			System.out.println("901 Incorrect number of arguments.");
		}
	}

	private void processPut() {
		String fileName = command[1];
		FileInputStream fileIn = null;
		try {
			File readFile = new File(fileName);
			fileIn = new FileInputStream(readFile);
			if (cmdConnection.sendStor(command[2])) {
				dataConnection.sendFile(fileIn);
				if (!cmdConnection.listenForMessage().startsWith("2")) {
					System.out.println("999 Processing error. File was not written for "
							+ "unknown reason.");
				}
			}			
		} catch (SecurityException e1) {
			System.out.println("910 Access to local file \"" + fileName + "\" denied.");
		} catch (FileNotFoundException e1) {
			System.out.println("999 Processing error. File \"" + fileName + "\" not found.");
		}  
	}

	private boolean processGet() {
		// Create file here in order to check for security exceptions
		FileOutputStream outStream = null;	
		try {
			File outFile = new File(command[2]);	
			outStream = new FileOutputStream(outFile);
			if (cmdConnection.requestFile(command[1])) {
				String response = cmdConnection.getResponseProcessor().listenDone();
				dataConnection.receiveFile(outStream, command); 
				System.out.println("<-- " + response);
			} 

		} catch (FileNotFoundException fe) {
			System.out.println("910 Access to local file \"" + 
					command[2] + "\" denied.");
			return false;
		} catch (SecurityException se) {
			System.out.println("910 Access to local file \"" + command[2] + "\" denied.");
			return false;
		}

		return false;
	}

	private boolean isValidDataCommand(String string) {
		return (command[0].toLowerCase().equals("dir") || 
				command[0].toLowerCase().equals("get") || 
				command[0].toLowerCase().equals("put") ||
				command[0].toLowerCase().equals("cd")); 

	}

	private boolean isCmdConnOpen() {
		return (cmdConnection != null &&
				(cmdConnection.getSocket() != null));

	}

	private void parseCommand(String input) {
		// This ignores any extraneous input
		command = null;
		input = input.trim();
		command = input.split("\\s+");

		for (int n = 0; n < command.length; n++) {
			command[n] = command[n].trim();
		}
	}

	private void processOpen() {
		command = Arrays.copyOf(command, 3);
		if (command[2] == null) {
			command[2] = "21"; // Default port
		}

		try {
			String server = command[1];
			int port = Integer.parseInt(command[2]);

			cmdConnection = new CmdConnection();
			dataConnection = new DataConnection(cmdConnection);
			cmdConnection.setDataConnection(dataConnection);
			cmdConnection.connect(server, port);
		} catch (NumberFormatException e) {
			System.out.println("902 Invalid argument.");
		}

	}

	private void processUser(String userName) {
		// send username over CmdConnection
		cmdConnection.login(userName);
	}
	
	/*
	 * Closes the command and data connections.
	 */
	public void closeConnections() {
		if (cmdConnection != null) {
			if (isCmdConnOpen()) {
				cmdConnection.sendQuit();
			}
			cmdConnection.close();
		}
		
		if (dataConnection != null) {
			dataConnection.close();
		}

		cmdConnection = null;
	}

	private String byteArrayToString (byte[] cmd) {

		// The string "<CR>" represents carriage return with no
		// other character input

		// Windows and *NIX compatible
		if (cmd[0] == 10 || cmd[0] == 13) {
			return new String("<CR>");
		} else {
			char[] cmdCharArray = new char[cmd.length];
			for (int index = 0; index < (cmd.length); index++) {
				cmdCharArray[index] = (char) cmd[index];
			}
			// Strip away \n and \r, return command as String.
			return new String(cmdCharArray).replace("\n", "").replace("\r", "");
		}
	}

	public DataConnection getDataConnection() {
		return dataConnection;
	}

}