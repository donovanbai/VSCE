import java.io.*;
import java.net.*;
 
public class ClientTest {
    public static void main(String[] args) throws IOException {        
 
        //String hostName = args[0];
        //int portNumber = Integer.parseInt(args[1]);
        //String hostName = "135.0.189.163";
        //InetAddress address = InetAddress.getByName("135.0.189.163");
        InetAddress hostIP = InetAddress.getByName("135.0.189.163");
        int portNumber = 10000;
 
        try (
            Socket echoSocket = new Socket(hostIP, portNumber);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))
        ) {
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                System.out.println("server reply: " + in.readLine());
            }
        } catch (UnknownHostException e) {
            System.err.println("Unknown host");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to host");
            System.exit(1);
        } 
    }
}
