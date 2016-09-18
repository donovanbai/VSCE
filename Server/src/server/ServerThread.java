package server;

import java.io.*;
import java.net.*;
import java.util.Map;

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
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

public class ServerThread extends Thread {
    private Socket socket;
    private DynamoDB dynamoDB;
    private String username;
    
    public ServerThread(Socket socket) {
        super("ServerThread");
        this.socket = socket;
    }
	
    public void run() {
	init();
	Table table = dynamoDB.getTable("users"); 
        //int portNumber = Integer.parseInt(args[0]);

        try ( 
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) { 
            while (true) {
                String inputLine = in.readLine();
                if (inputLine.equals("register")) {
                    username = in.readLine();
                    String pw = in.readLine();

                    if (username.equals("") || pw.equals("")) {
                        continue;
                    }

                    if (table.getItem("username", username) == null) {
                        Item newUser = new Item()
                            .withPrimaryKey("username", username)
                            .withString("password", pw)
                            .withNumber("balance", 1000000.00);
                        PutItemOutcome outcome = table.putItem(newUser);
                        System.out.println(socket.getInetAddress().getHostAddress() + " successfully created new user:");
                        System.out.println("\tusername: " + username);
                        System.out.println("\tpassword: " + pw);
                        out.println("0");
                    }
                    else {
                        out.println("1"); //username already exists
                    }
                    socket.close();
                    return;
                }
                else if (inputLine.equals("sign in")) {
                    username = in.readLine();
                    String pw = in.readLine();

                    if (username.equals("") || pw.equals("")) {
                        continue;
                    }

                    Item user = table.getItem("username", username);
                    if (user == null) {
                        out.println("1"); //username doesn't exist
                    }
                    else {
                        if (!(user.getString("password").equals(pw))) {
                            out.println("2"); //incorect password
                        }
                        else {
                            System.out.println(username + " logged in from " + socket.getInetAddress().getHostAddress());
                            out.println("0");
                            out.println(user.getDouble("balance"));

                            while (true) {
                                inputLine = in.readLine();
                                if (inputLine.equals("search stock")) {
                                    String stock = in.readLine();
                                    System.out.println(username + " searched for stock: " + stock);
                                    String stockPrice = null;
                                    try {
                                        stockPrice = getStockPrice(stock);
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                        out.println("fail");
                                        continue;
                                    }
                                    out.println(stockPrice);
                                }
                                else if (inputLine.equals("buy stock")) {
                                    String stock = in.readLine();
                                    int quantity = Integer.parseInt(in.readLine());
                                    String stockPrice = null;
                                    try {
                                        stockPrice = getStockPrice(stock);
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                        out.println("fail");
                                        continue;
                                    }
                                    if (stockPrice.equals("N/A")) continue;
                                    
                                    //add stock to user's account
                                    double cost = quantity * Double.parseDouble(stockPrice);
                                    double userBal = user.getDouble("balance");
                                    if (cost > userBal) {
                                        System.out.println(username + " tried to purchase too many shares");
                                        out.println(1);
                                        continue;
                                    }
                                    int prevQuantity;
                                    boolean b = user.isPresent(stock + "_stock"); //check if user already owns this stock
                                    if (b == false) {
                                        prevQuantity = 0;                               
                                    }
                                    else {
                                        prevQuantity = user.getInt(stock + "_stock");
                                    }
                                    //add stock to user's account and deduct balance
                                    double newBal = userBal - cost;
                                    UpdateItemSpec update = new UpdateItemSpec()
                                        .withPrimaryKey("username", username)
                                        .withUpdateExpression("set " + stock + "_stock = :v1, balance = :v2")
                                        .withValueMap(new ValueMap()
                                            .withNumber(":v1", prevQuantity + quantity)
                                            .withNumber(":v2", newBal));
                                    UpdateItemOutcome outcome = table.updateItem(update);
                                    //let client know their new balance
                                    System.out.println(username + " purchased " + Integer.toString(quantity) + " shares of " + stock);
                                    out.println(0);
                                    out.println(newBal);
                                }
                                else if (inputLine.equals("get profile")) {
                                    System.out.println(username + " is viewing their profile");
                                    Iterable<Map.Entry<String, Object>> i = user.attributes();
                                    for (Map.Entry e : i) {
                                        if (!(e.getKey().equals("username") || e.getKey().equals("password") || e.getKey().equals("balance"))) {
                                            out.println(e.getKey());
                                            out.println(e.getValue());
                                            String[] arr = e.getKey().toString().split("_");
                                            String name = arr[0];
                                            String type = arr[1];
                                            if (type.equals("stock")) {
                                                String stockPrice = null;
                                                try {
                                                    stockPrice = getStockPrice(name);
                                                } catch (Exception e2) {
                                                    System.out.print(e2.getMessage());
                                                }
                                                out.println(stockPrice);
                                            }
                                        }
                                    }
                                    out.println("end");
                                }
                                else if (inputLine.equals("logout")) {
                                    System.out.println(username + " logged out");
                                    socket.close();
                                    return;
                                }
                            }

                            //retrieve user data and send to client
                            /*
                            Iterable<Map.Entry<String, Object>> i = user.attributes();
                            for (Map.Entry e : i) {
                                if (!(e.getKey().equals("username") || e.getKey().equals("password"))) {
                                    out.println(e.getKey());
                                    out.println(e.getValue());
                                }
                            }
                            */
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(username + " disconnected");
        }
    }
	
    private void init() {
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
    
    private String getStockPrice(String stock) throws Exception {
        String strURL = "https://download.finance.yahoo.com/d/quotes.csv?s=" + stock + "&f=l1";
        URL stockURL = new URL(strURL);
        String stockPrice;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stockURL.openStream()))) {
            stockPrice = in.readLine(); //yahoo returns "N/A" if stock doesn't exist
        }
        return stockPrice;
    }
}
