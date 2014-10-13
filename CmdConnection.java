

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class CmdConnection {
	private ResponseProcessor responseProcessor;
	private CommandWriter speaker;
	private DataConnection dataConnection;
	private Socket cmdSocket;
	private boolean userLoggedIn = false;
	private static final int TIMEOUT_LIMIT = 30000;

	public CmdConnection() {
		responseProcessor = null;
		speaker = null;		
		cmdSocket = null;
		dataConnection = null;
		userLoggedIn = false;
	}

	public void setDataConnection(DataConnection dataConnection) {
		this.dataConnection = dataConnection;
	}

	public void connect(String hostName, int port) {
		try {
			cmdSocket = new Socket();
			// Commented out because of class instructions. We'd prefer not to have potential
			// infinite loops in the program.
			// cmdSocket.setSoTimeout(4*1000); // Set read timeout to 4 seconds.
			InetSocketAddress iNet = new InetSocketAddress(hostName, port);
			cmdSocket.connect(iNet, TIMEOUT_LIMIT);

			responseProcessor = new ResponseProcessor(cmdSocket, this);
			speaker = new CommandWriter(cmdSocket, this);

			responseProcessor.listen();

		} catch (UnknownHostException e) {
			System.out.println("920 Control connection to " + hostName
					+ " on port " + port + " failed to open.");
			close();
		} catch (SocketTimeoutException e) {
			System.out.println("920 Control connection to " + hostName
					+ " on port " + port + " failed to open.");
			close();
		} catch (IOException e) {
			System.out.println("920 Control connection to " + hostName
					+ " on port " + port + " failed to open.");
			close();
		}
	}

	public boolean isUserLoggedIn() {
		return userLoggedIn;
	}

	public void setUserLoggedIn(boolean userLoggedIn) {
		this.userLoggedIn = userLoggedIn;
	}

	public void login(String userName) {
		String message = "USER " + userName;
		sendMessage(message);
		String response = responseProcessor.listenPw();
		
		if (response.equals("331") || response.equals("332")) {
			getPassword();
		} else if (response.equals("230")) {
			setUserLoggedIn(true);
		}	
	}
	
	
	/*
	 * 
	 * ACCOUNT (ACCT)

            The argument field is a Telnet string identifying the user's
            account.  The command is not necessarily related to the USER
            command, as some sites may require an account for login and
            others only for specific access, such as storing files.  In
            the latter case the command may arrive at any time.

            There are reply codes to differentiate these cases for the
            automation: when account information is required for login,
            the response to a successful PASSword command is reply code
            332.  On the other hand, if account information is NOT
            required for login, the reply to a successful PASSword
            command is 230; and if the account information is needed for
            a command issued later in the dialogue, the server should
            return a 332 or 532 reply depending on whether it stores
            (pending receipt of the ACCounT command) or discards the
            command, respectively.
	 */
	
	// If string == PASS, then for login
	// If string == stor or otherwise, then for file access
	// EFFECTS: if receive 230 or 202, sets UserLoggedIn to true
	// 		    
	private void getAcct(String string) {
		BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
		
		try {
			System.out.print("Account: ");
			String account = bf.readLine();

			String response = sendAcct(account);
			if (response.equals("230") || response.equals("202")) {
				setUserLoggedIn(true);
			} else if (response.equals("") || response.equals("503") || response.equals(530)) {
				setUserLoggedIn(false);
			}

		} catch (IOException e) {
			System.err.println("998 Input error while reading commands, terminating.");
			close();
			System.exit(1);
		}
		
	}

	private void sendMessage(String message) {
		speaker.setMessage(message);
		speaker.write();	
	}

	public String sendPassword(String password) {
		String message = "PASS " + password;
		sendMessage(message);

		return responseProcessor.listenPw();
	}
	
	public String sendAcct(String account) {
		String message = "ACCT " + account;
		sendMessage(account);
		
		return responseProcessor.listenAcct();
		
	}

	// This method sends the PASV command to the server
	public boolean requestDataConn() {
		sendMessage("PASV");
		String response = responseProcessor.listenPASV();
		if (response == null) {
			return false;
		}
		return parseServConnectInfo(response); // Connects the dataConnection
	}

	// Process server response into IP address and port number of data connection offered
	private boolean parseServConnectInfo(String received) {
		String serverIP = "";
		String[] numbers = new String[6];
		// Pull out 6-tuple of IP and port
		String info = received.substring(received.indexOf('(') + 1,
				received.indexOf(')'));
		numbers = info.split(",");
		for (int i = 0; i < 4; i++) {
			serverIP += numbers[i];
			if (i < 3)
				serverIP += ".";
		}
		// Port number is sent as two 8-bit numbers
		try {
			int serverPort = Integer.parseInt(numbers[4])*256 + Integer.parseInt(numbers[5]);
			return dataConnection.connect(serverIP, serverPort);
		} catch (NumberFormatException e) {
			System.out.println("999 Processing error. The port number for the data connection "
					+ "could not be read."); // This is a received string. 
		}
		return false;
	}
	
	// This method sends the LIST command to the server
	public boolean sendList() {
		sendMessage("LIST");
		 return responseProcessor.listenDir();
	}
	
	// This method sends the TYPE I command to the server to request Binary Mode
	public boolean requestBin() {
		sendMessage("TYPE I");
		boolean isBin = responseProcessor.listenBin();
		dataConnection.setIsServerBinaryMode(isBin);
		return isBin;
	}

	// This method sends the STOR command to the server
	public boolean sendStor(String fileName) {	
		sendMessage("STOR " + fileName);
		return responseProcessor.listenStor();	
	}

	// This method sends the RETR command to the server
	public boolean requestFile(String fileName) {		
		sendMessage("RETR "+ fileName);
		return responseProcessor.listenGet();
	}
	
	public String listenForMessage() {
		return responseProcessor.listen();
	}

	// This method sends the CWD command to the server to change the current directory
	public void sendCd(String string) {
		String message = "CWD " + string;
		sendMessage(message);
		responseProcessor.listenCWD();
	}

	/*
	 * Prompts the user to enter a password and sends it to the server
	 */
	public void getPassword() {
		BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.print("Password: ");
			String password = bf.readLine();

			String response = sendPassword(password);
			if (response.equals("230")) {
				setUserLoggedIn(true);
			} else if (response.equals("332")) {
				getAcct("PASS");
			} else if (response.equals("")) {
				setUserLoggedIn(false);
			}

		} catch (IOException e) {
			System.err.println("998 Input error while reading commands, terminating.");
			close();
			System.exit(1);
		}
	}

	// This method sends the QUIT command to the server
	public void sendQuit() {
		if (cmdSocket != null) {
			String message = "QUIT";
			sendMessage(message);	
			responseProcessor.listen();
		}

		setUserLoggedIn(false);
	}

	/* 
	 * Closes the command socket and clears the CommandWriter
	 */
	public void close() {
		try { 
			if (cmdSocket != null) {
				cmdSocket.close();
				cmdSocket = null;
			}
			responseProcessor = null;
			speaker = null;
			setUserLoggedIn(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Socket getSocket() {
		return cmdSocket;
	}

	public ResponseProcessor getResponseProcessor() {
		return responseProcessor;
	}

}
