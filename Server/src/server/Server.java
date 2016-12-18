package server;

import java.io.IOException;
import java.net.ServerSocket;       

public class Server {
    public static void main(String[] args) {
        int portNumber = 10000;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (true) {
                new ServerThread(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}



