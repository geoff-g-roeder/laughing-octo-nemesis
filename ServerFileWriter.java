import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;


public class ServerFileWriter implements Runnable {

	private Socket socket = null;
	private DataConnection dataCon;
	private FileInputStream fileInputStream;
	private PrintStream controlSocket;

	public ServerFileWriter (Socket socket, DataConnection dataCon) {
		this.socket = socket;
		this.dataCon = dataCon;
		fileInputStream = null;
	}

	public PrintStream getControlSocket() {
		return controlSocket;
	}

	public void setpSocket(PrintStream pSocket) {
		this.controlSocket = pSocket;
	}

	@Override
	public void run() {
		try {	
			byte[] bytesArray = new byte[4096];
			int bytesRead = 0;
			DataOutputStream ds = new DataOutputStream(socket.getOutputStream());

			while ((bytesRead = fileInputStream.read(bytesArray)) != -1) {
				ds.write(bytesArray, 0, bytesRead);
				System.out.println("Sent: " +  bytesRead + " bytes");
			}		
			fileInputStream.close();
			ds.close();
			fileInputStream = null;
			ds = null;
		
		} catch (IOException e) {
			System.out.println("935 Data transfer connection I/O error "
					+ ", closing data connection.");
			dataCon.close();
		} 		
	}

	public void setFileInStream(FileInputStream fileInputStream) {
		this.fileInputStream = fileInputStream;		
	}


}
