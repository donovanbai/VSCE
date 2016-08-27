import java.io.*;
import java.net.*;

public class ServerTest {
    public static void main(String[] args) {
        //int portNumber = Integer.parseInt(args[0]);
        int portNumber = 10000;
		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println("IOException while creating serverSocket");
		}

		while (true) {
			try ( 
				Socket clientSocket = serverSocket.accept();
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			) { 
				System.out.println(clientSocket.getInetAddress());
				String inputLine, outputLine;
				while ((inputLine = in.readLine()) != null) {
					System.out.println(inputLine);
					outputLine = "length of string: " + inputLine.length();
					out.println(outputLine);
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
    }
}
