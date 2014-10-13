
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public class CommandWriter {

	private String message;
	private static final String NEWLINE = "\r\n";

	private PrintStream controlWriter;

	/*
	 * Creates a CommandWriter with a PrintStream using the given Socket and
	 * Command Connection 
	 */
	public CommandWriter (Socket socket, CmdConnection cmdCon) {
		message = null;

		try {
			controlWriter = new PrintStream(socket.getOutputStream());

		} catch (IOException e) {
			System.out.println("925 Control connection I/O error, "
					+ "closing control connection.");
			cmdCon.close();
		}
	}

	public PrintStream getControlSocket() {
		return controlWriter;
	}

	public void setpSocket(PrintStream pSocket) {
		this.controlWriter = pSocket;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	// Sends the current message to the server over the Socket connection
	public void write() {

		if (message != null) {
			controlWriter.print(message + NEWLINE);
			controlWriter.flush(); 
			System.out.println("--> " + message);
		}
		message = null;
	}

}



