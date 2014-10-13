import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


public class ServerDataReader implements Runnable {

	private Socket socket;
	private String[] commands = null;
	private CmdConnection cmdConnection;
	private DataConnection dataConnection;
	private FileOutputStream outStream = null;


	public ServerDataReader(Socket socket, CmdConnection cmdConnection, DataConnection dataConnection) {
		this.socket = socket;
		this.cmdConnection = cmdConnection;
		this.dataConnection = dataConnection;
	}

	@Override
	public void run() {

		try{
			if (commands[0].equals("get")) {
				if (outStream == null) {
					throw new NullPointerException();
				}

				DataInputStream ds = new DataInputStream(socket.getInputStream());

				try {
					byte[] bytesArray = new byte[4096];
					int bytesRead = -1;
					while ((bytesRead = ds.read(bytesArray)) != -1) {
						outStream.write(bytesArray, 0, bytesRead);
					}	
					outStream.close();

				} catch (NullPointerException e) {

					// 999 error thrown if File is null.
					System.out.println("");
					System.out.println("999 Processing error. The file could not be sent.");
				}

			} else {
				// Command is dir
				BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				String receivedMessage;
				while ((receivedMessage = socketReader.readLine()) != null) {	
					System.out.println(receivedMessage);
				}
			}

		} catch (IOException e) {
			System.out.println("935 Data transfer connection I/O error, "
					+ "closing data connection.");
		} finally {	
			dataConnection.close();			
			//cmdConnection.getResponseProcessor().listen(); // Listens for completion message
			
//			if (commands[0].equals("get")) {
//				System.out.println("");
//				System.out.print("csftp> ");
//			}		
		}
	}

	public void setCommandString(String[] commandString) {
		this.commands = commandString;
	}


	public void setOutStream(FileOutputStream outStream) {
		this.outStream = outStream;
	}

}
