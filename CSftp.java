
import java.lang.System;
import java.io.IOException;
//
// This is an implementation of a simplified version of a command 
// line ftp client. The program takes no arguments.
//
public class CSftp
{
	static final int MAX_LEN = 255;
	private InputProcessor inputProcessor;

	public static void main(String [] args)
	{
		CSftp client = new CSftp();
		client.inputProcessor = new InputProcessor();
		client.readInput();
	}
	
	public synchronized void readInput() {
		byte cmdString[] = new byte[MAX_LEN];
		boolean quit = false;
		try {
			for (int len = 1; len > 0;) {
				System.out.print("csftp> ");
				len = System.in.read(cmdString);
				if (len <= 0) 
					break;
				// Start processing the command here.			
				quit = inputProcessor.processCommand(cmdString);
				if (quit) {
					inputProcessor.closeConnections();
					break;
				}
			}
		} catch (IOException exception) {
			System.err.println("998 Input error while reading commands, terminating.");
		} finally {
			inputProcessor.closeConnections();
			System.exit(0);
		}		
	}		
}

