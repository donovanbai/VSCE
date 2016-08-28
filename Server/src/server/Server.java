package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;

public class Server {
    static DynamoDB dynamoDB;
	
    public static void main(String[] args) {
	init();
	Table table = dynamoDB.getTable("users"); 
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
		String inputLine = in.readLine();
		if (inputLine.equals("register")) {
                    String username = in.readLine();
                    String pw = in.readLine();

                    if (table.getItem("username", username) == null) {
			Item newUser = new Item()
                            .withPrimaryKey("username", username)
                            .withString("password", pw);
			PutItemOutcome outcome = table.putItem(newUser);
			System.out.println("successfully created new user:");
			System.out.println("username: " + username);
			System.out.println("password: " + pw);
			out.println("0");
                    }
                    else {
			System.out.println("failed registration");
			out.println("1");
                    }
		}
            } catch (IOException e) {
		System.out.println(e.getMessage());
            }
	}
    }
	
    private static void init() {
        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                "Cannot load the credentials from the credential profiles file. " +
                "Please make sure that your credentials file is at the correct " +
                "location (~/.aws/credentials), and is in valid format.",
            e);
        }
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        client.setRegion(usWest2);
        dynamoDB = new DynamoDB(client);
    }
}
