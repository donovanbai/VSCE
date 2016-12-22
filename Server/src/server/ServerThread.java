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
import com.google.gson.Gson;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ServerThread extends Thread {
    private Socket socket;
    private DynamoDB dynamoDB;
    private String username;
    
    public ServerThread(Socket socket) {
        super("ServerThread");
        this.socket = socket;
    }
	
    public void run() {
	init(); // initialize connection to amazon dynamodb
	Table table = dynamoDB.getTable("users"); 
        try ( 
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) { 
            while (true) { // process client requests
                String inputLine = in.readLine();
                if (inputLine.equals("register")) { // account registration
                    username = in.readLine();
                    String pw = in.readLine();
                    if (username.equals("") || pw.equals("")) {
                        socket.close();
                        return;
                    }
                    if (table.getItem("username", username) != null) out.println("1"); // username already exists
                    else { 
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
                    socket.close();
                    return;
                }
                else if (inputLine.equals("sign in")) { // client login
                    username = in.readLine();
                    String pw = in.readLine();
                    if (username.equals("") || pw.equals("")) {
                        socket.close();
                        return;
                    }
                    Item user = table.getItem("username", username);
                    if (user == null) { // username doesn't exist
                        out.println("1");
                        socket.close();
                        return;
                    }
                    if (!(user.getString("password").equals(pw))) { //incorrect password
                        out.println("2");
                        socket.close();
                        return;
                    }
                    System.out.println(username + " logged in from " + socket.getInetAddress().getHostAddress());
                    out.println("0");
                    out.println(user.getString("balance")); // use getString because getDouble is not exact
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
                            if (stockPrice.equals("N/A")) {
                                out.println("N/A");
                                continue;
                            }

                            BigDecimal stockPriceBigDec = new BigDecimal(stockPrice);
                            BigDecimal cost = stockPriceBigDec.multiply(new BigDecimal(quantity));
                            // cost could have more than 2 decimal places so round up
                            cost = cost.setScale(2, RoundingMode.UP);
                            BigDecimal userBal = new BigDecimal(user.getString("balance"));
                            if (cost.compareTo(userBal) == 1) { // if cost > userBal
                                System.out.println(username + " tried to purchase too many shares");
                                out.println(1);
                                continue;
                            }
                            int prevQuantity;
                            if (!user.isPresent(stock + "_stock")) { //check if user already owns this stock
                                prevQuantity = 0;                               
                            }
                            else {
                                prevQuantity = user.getInt(stock + "_stock");
                            }
                            //add stock to user's account and deduct balance
                            BigDecimal newBal = userBal.subtract(cost);
                            UpdateItemSpec update = new UpdateItemSpec()
                                .withPrimaryKey("username", username)
                                .withUpdateExpression("set " + stock + "_stock = :v1, balance = :v2")
                                .withValueMap(new ValueMap()
                                    .withNumber(":v1", prevQuantity + quantity)
                                    .withNumber(":v2", newBal));
                            UpdateItemOutcome outcome = table.updateItem(update);
                            user = table.getItem("username", username); // fetch updated user
                            // logging
                            System.out.println(username + " purchased " + Integer.toString(quantity) + " shares of " + stock);
                            System.out.println(username + "'s new balance: " + newBal);
                            // let client know transaction succeeded and send their new balance
                            out.println(0);
                            out.println(newBal);
                        }
                        else if (inputLine.equals("sell stock")) {
                            String stock = in.readLine();
                            int quantity = Integer.parseInt(in.readLine());
                            int quantityOwned = user.getInt(stock + "_stock");
                            if (quantity > quantityOwned) {
                                System.out.println(username + " tried to sell too many shares");
                                out.println(1);
                                continue;
                            }
                            String stockPrice = null;
                            try {
                                stockPrice = getStockPrice(stock);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                out.println("fail");
                                continue;
                            }
                            if (stockPrice.equals("N/A")) {
                                out.println("N/A");
                                continue;
                            }

                            BigDecimal stockPriceBigDec = new BigDecimal(stockPrice);
                            BigDecimal total = stockPriceBigDec.multiply(new BigDecimal(quantity));
                            // total could have more than 2 decimal places so round up
                            total = total.setScale(2, RoundingMode.UP);
                            BigDecimal userBal = new BigDecimal(user.getString("balance"));
                            //deduct stock from user's account and add balance
                            BigDecimal newBal = userBal.add(total);
                            UpdateItemSpec update = new UpdateItemSpec()
                                .withPrimaryKey("username", username)
                                .withUpdateExpression("set " + stock + "_stock = :v1, balance = :v2")
                                .withValueMap(new ValueMap()
                                    .withNumber(":v1", quantityOwned - quantity)
                                    .withNumber(":v2", newBal));
                            UpdateItemOutcome outcome = table.updateItem(update);
                            user = table.getItem("username", username); // fetch updated user
                            if (user.getInt(stock + "_stock") == 0) {
                                update = new UpdateItemSpec()
                                    .withPrimaryKey("username", username)
                                    .withUpdateExpression("remove " + stock + "_stock");
                                outcome = table.updateItem(update);
                                user = table.getItem("username", username);
                            }
                            // logging
                            System.out.println(username + " sold " + Integer.toString(quantity) + " shares of " + stock);
                            System.out.println(username + "'s new balance: " + newBal);
                            // let client know transaction succeeded, send their new balance and new quantity
                            out.println(0);
                            out.println(newBal);
                            out.println(quantityOwned - quantity);
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
                                    if (arr.length == 2) {
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
                                    else if (name == "btc") {
                                        String price = null;
                                        try {
                                            price = getBtcPrice();
                                        } catch (Exception e2) {
                                            System.out.print(e2.getMessage());
                                        }
                                        out.println(price);
                                    }
                                }
                            }
                            out.println("end");
                        }
                        else if (inputLine.equals("get btc price")) {
                            System.out.println(username + " is getting bitcoin price");
                            String price = null;
                            try {
                                price = getBtcPrice();
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                out.println("fail");
                                continue;
                            }
                            out.println(price);
                        }
                        else if (inputLine.equals("buy btc")) {
                            BigDecimal amount = new BigDecimal(in.readLine());
                            String strBal = user.getString("balance");
                            BigDecimal userBal = new BigDecimal(strBal);
                            if (amount.compareTo(userBal) == 1) {
                                System.out.println(username + " tried to buy too much bitcoin");
                                out.println(1);
                                continue;
                            }
                            String strPrice = null;
                            try {
                                strPrice = getBtcPrice();
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                out.println("fail");
                                continue;
                            }
                            BigDecimal price = new BigDecimal(strPrice);
                            BigDecimal btcQuantity = amount.divide(price, 8, RoundingMode.DOWN);
                            BigDecimal prevQuantity;
                            if (!user.isPresent("btc")) {
                                prevQuantity = new BigDecimal("0");                               
                            }
                            else {
                                prevQuantity = new BigDecimal(user.getString("btc"));
                            }
                            UpdateItemSpec update = new UpdateItemSpec()
                                .withPrimaryKey("username", username)
                                .withUpdateExpression("set btc = :v1, balance = :v2")
                                .withValueMap(new ValueMap()
                                    .withNumber(":v1", btcQuantity.add(prevQuantity))
                                    .withNumber(":v2", userBal.subtract(amount)));
                            UpdateItemOutcome outcome = table.updateItem(update);
                            user = table.getItem("username", username); // fetch updated user
                            out.println(0);
                            out.println(btcQuantity.add(prevQuantity));
                            out.println(userBal.subtract(amount));
                        }
                        else if (inputLine.equals("logout")) {
                            System.out.println(username + " logged out");
                            socket.close();
                            return;
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
    
    private String getBtcPrice() throws Exception {
        String strURL = "https://api.quadrigacx.com/v2/ticker?book=btc_usd";
        URL btcURL = new URL(strURL);
        URLConnection btcURLConnection = btcURL.openConnection();
        //server returns 403 if user agent is not set
        btcURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
        String price;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(btcURLConnection.getInputStream()))) {
            Gson gson = new Gson(); // parse JSON reply
            BtcPrice btcPrice = gson.fromJson(in.readLine(), BtcPrice.class);
            price = btcPrice.last.toString();
        }
        return price;
    }
}
