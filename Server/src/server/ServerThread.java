package server;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

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
                        table.putItem(newUser);
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
                        if (inputLine == null) {
                            System.out.println(username + " disconnected");
                            socket.close();
                            return;
                        }
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

                            // calculate cost (2 decimal places) and check if user has enough balance
                            BigDecimal stockPriceBigDec = new BigDecimal(stockPrice);
                            BigDecimal cost = stockPriceBigDec.multiply(new BigDecimal(quantity));
                            cost = cost.setScale(2, RoundingMode.UP);
                            BigDecimal userBal = new BigDecimal(user.getString("balance"));
                            if (cost.compareTo(userBal) == 1) { // if cost > userBal
                                System.out.println(username + " tried to purchase too many shares");
                                out.println(1);
                                continue;
                            }
                            // calculate new balance
                            BigDecimal newBal = userBal.subtract(cost);

                            // calculate new quantity
                            int prevQuantity;
                            if (!user.isPresent(stock + "_stock")) { //check if user already owns this stock
                                prevQuantity = 0;                               
                            }
                            else {
                                prevQuantity = user.getInt(stock + "_stock");
                            }
                            int newQuantity = prevQuantity + quantity;
                            // keep track of how much the user spent on this stock (for calculating gain/loss)
                            BigDecimal newOrig;
                            if (!user.isPresent(stock + "_stock_orig")) newOrig = cost;
                            else {
                                BigDecimal oldOrig = new BigDecimal(user.getString(stock + "_stock_orig"));
                                newOrig = oldOrig.add(cost);
                            }
                            // update user
                            UpdateItemSpec update = new UpdateItemSpec()
                                .withPrimaryKey("username", username)
                                .withUpdateExpression("set " + stock + "_stock = :v1, balance = :v2, " + stock +
                                "_stock_orig = :v3")
                                .withValueMap(new ValueMap()
                                    .withNumber(":v1", newQuantity)
                                    .withNumber(":v2", newBal)
                                    .withNumber(":v3", newOrig));
                            table.updateItem(update);
                            user = table.getItem("username", username); // fetch updated user
                            // logging
                            System.out.println(username + " purchased " + Integer.toString(quantity) + " shares of " +
                            stock);
                            System.out.println(username + "'s new balance: " + newBal);
                            // send reply to client
                            out.println(0);
                            out.println(newBal);
                        }
                        // OPTIMIZE THIS AND OTHER SELL CODE LATER
                        else if (inputLine.equals("sell stock")) {
                            String stock = in.readLine();
                            BigDecimal quantity = new BigDecimal(in.readLine());
                            BigDecimal quantityOwned = new BigDecimal(user.getInt(stock + "_stock"));
                            if (quantity.compareTo(quantityOwned) == 1) {
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
                            BigDecimal total = stockPriceBigDec.multiply(quantity);
                            // total could have more than 2 decimal places so round up
                            total = total.setScale(2, RoundingMode.UP);
                            BigDecimal userBal = new BigDecimal(user.getString("balance"));
                            // deduct stock from user's account and add balance
                            BigDecimal newBal = userBal.add(total);
                            BigDecimal newQuantity = quantityOwned.subtract(quantity);
                            UpdateItemSpec update = new UpdateItemSpec()
                                .withPrimaryKey("username", username)
                                .withUpdateExpression("set " + stock + "_stock = :v1, balance = :v2")
                                .withValueMap(new ValueMap()
                                    .withNumber(":v1", newQuantity)
                                    .withNumber(":v2", newBal));
                            table.updateItem(update);
                            user = table.getItem("username", username); // fetch updated user
                            BigDecimal newOrig = null;
                            if (user.getInt(stock + "_stock") == 0) {
                                // remove stock and money spent on stock from database
                                update = new UpdateItemSpec()
                                    .withPrimaryKey("username", username)
                                    .withUpdateExpression("remove " + stock + "_stock, " + stock + "_stock_orig");
                                table.updateItem(update);
                            }
                            else {
                                // update money spent on stock
                                // eg. if user had spent $10000 on 100 shares
                                // selling 50% of shares means reducing $10000 by 50%
                                BigDecimal remainingPct = newQuantity.divide(quantityOwned, 10, RoundingMode.HALF_UP);
                                BigDecimal oldOrig = new BigDecimal(user.getString(stock + "_stock_orig"));
                                newOrig = remainingPct.multiply(oldOrig);
                                newOrig = newOrig.setScale(2, RoundingMode.HALF_UP);
                                update = new UpdateItemSpec()
                                    .withPrimaryKey("username", username)
                                    .withUpdateExpression("set " + stock + "_stock_orig = :v1")
                                    .withValueMap(new ValueMap()
                                        .withNumber(":v1", newOrig));
                                table.updateItem(update);
                            }
                            user = table.getItem("username", username);
                            // logging
                            System.out.println(username + " sold " + quantity + " shares of " + stock);
                            System.out.println(username + "'s new balance: " + newBal);
                            // let client know transaction succeeded, send their new balance and new quantity
                            out.println(0);
                            out.println(newBal);
                            out.println(newQuantity);
                            if (user.isPresent(stock + "_stock")) out.println(newOrig);
                        }
                        else if (inputLine.equals("get profile")) {
                            System.out.println(username + " is viewing their profile");
                            Iterable<Map.Entry<String, Object>> i = user.attributes();
                            for (Map.Entry e : i) {
                                if (!(e.getKey().equals("username") || e.getKey().equals("password") ||
                                e.getKey().equals("balance"))) {
                                    String[] arr = e.getKey().toString().split("_");
                                    if (arr[arr.length-1].equals("orig")) continue;
                                    String name = arr[0];
                                    if (arr.length == 2) {
                                        out.println(e.getKey());
                                        out.println(e.getValue());
                                        String type = arr[1];
                                        String price = null;
                                        if (type.equals("stock")) {
                                            try {
                                                price = getStockPrice(name);
                                            } catch (Exception e2) {
                                                System.out.print(e2.getMessage());
                                            }

                                        }
                                        else if (type.equals("cur")) {
                                            try {
                                                price = getCurrencyExchangeRate(name);
                                            } catch (Exception e2) {
                                                System.out.print(e2.getMessage());
                                            }
                                        }
                                        out.println(price);
                                        out.println(user.getString(e.getKey().toString() + "_orig"));
                                    }
                                    else if (name.equals("btc")) {
                                        out.println(e.getKey());
                                        out.println(e.getValue());
                                        String price = null;
                                        try {
                                            price = getBtcPrice();
                                        } catch (Exception e2) {
                                            System.out.print(e2.getMessage());
                                        }
                                        out.println(price);
                                        out.println(user.getString(e.getKey().toString() + "_orig"));
                                    }
                                    else if (name.equals("eth")) {
                                        out.println(e.getKey());
                                        out.println(e.getValue());
                                        String price = null;
                                        try {
                                            price = getEthPrice();
                                        } catch (Exception e2) {
                                            System.out.print(e2.getMessage());
                                        }
                                        out.println(price);
                                        out.println(user.getString(e.getKey().toString() + "_orig"));
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
                        else if (inputLine.equals("get eth price")) {
                            System.out.println(username + " is getting ether price");
                            String price = null;
                            try {
                                price = getEthPrice();
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
                            // calculate new balance
                            BigDecimal newBal = userBal.subtract(amount);
                            String strPrice = null;
                            try {
                                strPrice = getBtcPrice();
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                out.println("fail");
                                continue;
                            }
                            BigDecimal price = new BigDecimal(strPrice);
                            // calculate new quantity
                            BigDecimal btcQuantity = amount.divide(price, 8, RoundingMode.DOWN);
                            BigDecimal prevQuantity;
                            if (!user.isPresent("btc")) {
                                prevQuantity = new BigDecimal("0");                               
                            }
                            else {
                                prevQuantity = new BigDecimal(user.getString("btc"));
                            }
                            BigDecimal newQuantity = btcQuantity.add(prevQuantity);
                            // keep track of how much the user spent on btc (for calculating gain/loss)
                            BigDecimal newOrig;
                            if (!user.isPresent("btc_orig")) newOrig = amount;
                            else {
                                BigDecimal oldOrig = new BigDecimal(user.getString("btc_orig"));
                                newOrig = oldOrig.add(amount);
                            }
                            // update user
                            UpdateItemSpec update = new UpdateItemSpec()
                                .withPrimaryKey("username", username)
                                .withUpdateExpression("set btc = :v1, balance = :v2, btc_orig = :v3")
                                .withValueMap(new ValueMap()
                                    .withNumber(":v1", newQuantity)
                                    .withNumber(":v2", newBal)
                                    .withNumber(":v3", newOrig));
                            table.updateItem(update);
                            user = table.getItem("username", username); // fetch updated user
                            // send reply to client
                            out.println(0);
                            out.println(btcQuantity.add(prevQuantity));
                            out.println(userBal.subtract(amount));
                        }
                        else if (inputLine.equals("buy eth")) {
                            BigDecimal amount = new BigDecimal(in.readLine());
                            String strBal = user.getString("balance");
                            BigDecimal userBal = new BigDecimal(strBal);
                            if (amount.compareTo(userBal) == 1) {
                                System.out.println(username + " tried to buy too much bitcoin");
                                out.println(1);
                                continue;
                            }
                            // calculate new balance
                            BigDecimal newBal = userBal.subtract(amount);
                            String strPrice = null;
                            try {
                                strPrice = getEthPrice();
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                out.println("fail");
                                continue;
                            }
                            BigDecimal price = new BigDecimal(strPrice);
                            // calculate new quantity
                            BigDecimal ethQuantity = amount.divide(price, 8, RoundingMode.DOWN);
                            BigDecimal prevQuantity;
                            if (!user.isPresent("eth")) {
                                prevQuantity = new BigDecimal("0");                               
                            }
                            else {
                                prevQuantity = new BigDecimal(user.getString("eth"));
                            }
                            BigDecimal newQuantity = ethQuantity.add(prevQuantity);
                            // keep track of how much the user spent on eth (for calculating gain/loss)
                            BigDecimal newOrig;
                            if (!user.isPresent("eth_orig")) newOrig = amount;
                            else {
                                BigDecimal oldOrig = new BigDecimal(user.getString("eth_orig"));
                                newOrig = oldOrig.add(amount);
                            }
                            // update user
                            UpdateItemSpec update = new UpdateItemSpec()
                                .withPrimaryKey("username", username)
                                .withUpdateExpression("set eth = :v1, balance = :v2, eth_orig = :v3")
                                .withValueMap(new ValueMap()
                                    .withNumber(":v1", newQuantity)
                                    .withNumber(":v2", newBal)
                                    .withNumber(":v3", newOrig));
                            table.updateItem(update);
                            user = table.getItem("username", username); // fetch updated user
                            // send reply to client
                            out.println(0);
                            out.println(ethQuantity.add(prevQuantity));
                            out.println(userBal.subtract(amount));
                        }
                        else if (inputLine.equals("sell btc")) {
                            BigDecimal quantity = new BigDecimal(in.readLine());
                            BigDecimal quantityOwned = new BigDecimal(user.getString("btc"));
                            if (quantity.compareTo(quantityOwned) == 1) {
                                System.out.println(username + " tried to sell too much bitcoin");
                                out.println(1);
                                continue;
                            }   
                            BigDecimal price;
                            try {
                                price = new BigDecimal(getBtcPrice());
                            } catch (Exception e) {
                                System.out.println("failed to retrieve bitcoin price");
                                out.println("fail");
                                continue;
                            }
                            BigDecimal bal = new BigDecimal(user.getString("balance"));
                            BigDecimal newBal = bal.add(quantity.multiply(price));
                            newBal = newBal.setScale(2, RoundingMode.DOWN);
                            BigDecimal newQuantity = quantityOwned.subtract(quantity);
                            UpdateItemSpec update = new UpdateItemSpec()
                                .withPrimaryKey("username", username)
                                .withUpdateExpression("set btc = :v1, balance = :v2")
                                .withValueMap(new ValueMap()
                                    .withNumber(":v1", newQuantity)
                                    .withNumber(":v2", newBal));
                            table.updateItem(update);
                            user = table.getItem("username", username); // fetch updated user
                            BigDecimal newOrig = null;
                            if (user.getString("btc").equals("0")) {
                                // remove btc and money spent on btc from database
                                update = new UpdateItemSpec()
                                    .withPrimaryKey("username", username)
                                    .withUpdateExpression("remove btc, btc_orig");
                                table.updateItem(update);
                            }
                            else {
                                // update money spent on btc
                                BigDecimal remainingPct = newQuantity.divide(quantityOwned, 10, RoundingMode.HALF_UP);
                                BigDecimal oldOrig = new BigDecimal(user.getString("btc_orig"));
                                newOrig = remainingPct.multiply(oldOrig);
                                newOrig = newOrig.setScale(2, RoundingMode.HALF_UP);
                                update = new UpdateItemSpec()
                                    .withPrimaryKey("username", username)
                                    .withUpdateExpression("set btc_orig = :v1")
                                    .withValueMap(new ValueMap()
                                        .withNumber(":v1", newOrig));
                                table.updateItem(update);
                            }
                            user = table.getItem("username", username);
                            out.println(0);
                            out.println(quantityOwned.subtract(quantity));
                            out.println(newBal);
                            if (user.isPresent("btc")) out.println(newOrig);
                        }
                        else if (inputLine.equals("sell eth")) {
                            BigDecimal quantity = new BigDecimal(in.readLine());
                            BigDecimal quantityOwned = new BigDecimal(user.getString("eth"));
                            if (quantity.compareTo(quantityOwned) == 1) {
                                System.out.println(username + " tried to sell too much ether");
                                out.println(1);
                                continue;
                            }   
                            BigDecimal price;
                            try {
                                price = new BigDecimal(getEthPrice());
                            } catch (Exception e) {
                                System.out.println("failed to retrieve ether price");
                                out.println("fail");
                                continue;
                            }
                            BigDecimal bal = new BigDecimal(user.getString("balance"));
                            BigDecimal newBal = bal.add(quantity.multiply(price));
                            newBal = newBal.setScale(2, RoundingMode.DOWN);
                            BigDecimal newQuantity = quantityOwned.subtract(quantity);
                            UpdateItemSpec update = new UpdateItemSpec()
                                .withPrimaryKey("username", username)
                                .withUpdateExpression("set eth = :v1, balance = :v2")
                                .withValueMap(new ValueMap()
                                    .withNumber(":v1", newQuantity)
                                    .withNumber(":v2", newBal));
                            table.updateItem(update);
                            user = table.getItem("username", username); // fetch updated user
                            BigDecimal newOrig = null;
                            if (user.getString("eth").equals("0")) {
                                // remove eth and money spent on eth from database
                                update = new UpdateItemSpec()
                                    .withPrimaryKey("username", username)
                                    .withUpdateExpression("remove eth, eth_orig");
                                table.updateItem(update);
                            }
                            else {
                                // update money spent on eth
                                BigDecimal remainingPct = newQuantity.divide(quantityOwned, 10, RoundingMode.HALF_UP);
                                BigDecimal oldOrig = new BigDecimal(user.getString("eth_orig"));
                                newOrig = remainingPct.multiply(oldOrig);
                                newOrig = newOrig.setScale(2, RoundingMode.HALF_UP);
                                update = new UpdateItemSpec()
                                    .withPrimaryKey("username", username)
                                    .withUpdateExpression("set eth_orig = :v1")
                                    .withValueMap(new ValueMap()
                                        .withNumber(":v1", newOrig));
                                table.updateItem(update);
                            }
                            user = table.getItem("username", username);
                            out.println(0);
                            out.println(quantityOwned.subtract(quantity));
                            out.println(newBal);
                            if (user.isPresent("eth")) out.println(newOrig);
                        }
                        else if (inputLine.equals("search currency")) {
                            String currency = in.readLine();
                            System.out.println(username + " searched for currency: " + currency);
                            String curToUsd;
                            try {
                                curToUsd = getCurrencyExchangeRate(currency);
                            } catch (Exception e) {
                                System.out.println("failed to retrieve currency exchange rate");
                                out.println("fail");
                                continue;
                            }
                            out.println(curToUsd);
                        }
                        else if (inputLine.equals("buy currency")) {
                            String currency = in.readLine().toUpperCase(); // dynamoDB is case-sensitive
                            BigDecimal quantity = new BigDecimal(in.readLine());
                            String curToUsdStr = null;
                            try {
                                curToUsdStr = getCurrencyExchangeRate(currency);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                out.println("fail");
                                continue;
                            }

                            BigDecimal curToUsd = new BigDecimal(curToUsdStr);
                            BigDecimal cost = curToUsd.multiply(quantity);
                            // cost could have more than 2 decimal places so round up
                            cost = cost.setScale(2, RoundingMode.UP);
                            BigDecimal userBal = new BigDecimal(user.getString("balance"));
                            if (cost.compareTo(userBal) == 1) {
                                System.out.println(username + " tried to purchase too much currency");
                                out.println(1);
                                continue;
                            }
                            // calculate new balance
                            BigDecimal newBal = userBal.subtract(cost);
                            // calculate new quantity
                            BigDecimal prevQuantity;
                            if (!user.isPresent(currency + "_cur")) { //check if user already owns this currency
                                prevQuantity = new BigDecimal("0");
                            }
                            else {
                                prevQuantity = new BigDecimal(user.getString(currency + "_cur"));
                            }
                            BigDecimal newQuantity = prevQuantity.add(quantity);
                            // keep track of how much the user spent on this currency (for calculating gain/loss)
                            BigDecimal newOrig;
                            if (!user.isPresent(currency + "_cur_orig")) newOrig = cost;
                            else {
                                BigDecimal oldOrig = new BigDecimal(user.getString(currency + "_cur_orig"));
                                newOrig = oldOrig.add(cost);
                            }
                            // update user
                            UpdateItemSpec update = new UpdateItemSpec()
                                .withPrimaryKey("username", username)
                                .withUpdateExpression("set " + currency + "_cur = :v1, balance = :v2, " + currency +
                                    "_cur_orig = :v3")
                                .withValueMap(new ValueMap()
                                    .withNumber(":v1", newQuantity)
                                    .withNumber(":v2", newBal)
                                    .withNumber(":v3", newOrig));
                            table.updateItem(update);
                            user = table.getItem("username", username); // fetch updated user
                            // logging
                            System.out.println(username + " purchased " + quantity + " " + currency);
                            System.out.println(username + "'s new balance: " + newBal);
                            // send reply to client
                            out.println(0);
                            out.println(newBal);
                        }
                        else if (inputLine.equals("sell currency")) {
                            String currency = in.readLine().toUpperCase();
                            BigDecimal quantity = new BigDecimal(in.readLine());
                            BigDecimal quantityOwned = new BigDecimal(user.getString(currency + "_cur"));
                            if (quantity.compareTo(quantityOwned) == 1) {
                                System.out.println(username + " tried to sell too much " + currency);
                                out.println(1);
                                continue;
                            }
                            BigDecimal curToUsd;
                            try {
                                curToUsd = new BigDecimal(getCurrencyExchangeRate(currency));
                            } catch (Exception e) {
                                System.out.println("failed to retrieve exchange rate for " + currency);
                                out.println("fail");
                                continue;
                            }
                            BigDecimal bal = new BigDecimal(user.getString("balance"));
                            BigDecimal newBal = bal.add(quantity.multiply(curToUsd));
                            newBal = newBal.setScale(2, RoundingMode.DOWN);
                            BigDecimal newQuantity = quantityOwned.subtract(quantity);
                            BigDecimal newOrig = null;
                            if (newQuantity.compareTo(new BigDecimal("0")) == 0) {
                                UpdateItemSpec update = new UpdateItemSpec()
                                    .withPrimaryKey("username", username)
                                    .withUpdateExpression("set balance = :v1 remove " + currency + "_cur, " + currency +
                                    "_cur_orig")
                                    .withValueMap(new ValueMap()
                                        .withNumber(":v1", newBal));
                                table.updateItem(update);
                            }
                            else {
                                // update money spent on currency
                                BigDecimal remainingPct = newQuantity.divide(quantityOwned, 10, RoundingMode.HALF_UP);
                                BigDecimal oldOrig = new BigDecimal(user.getString(currency + "_cur_orig"));
                                newOrig = remainingPct.multiply(oldOrig);
                                newOrig = newOrig.setScale(2, RoundingMode.HALF_UP);
                                UpdateItemSpec update = new UpdateItemSpec()
                                    .withPrimaryKey("username", username)
                                    .withUpdateExpression("set balance = :v1, " + currency + "_cur = :v2, " +
                                    currency + "_cur_orig = :v3")
                                    .withValueMap(new ValueMap()
                                        .withNumber(":v1", newBal)
                                        .withNumber(":v2", newQuantity)
                                        .withNumber(":v3", newOrig));
                                table.updateItem(update);
                            }
                            user = table.getItem("username", username);
                            out.println(0);
                            out.println(newQuantity);
                            out.println(newBal);
                            if (user.isPresent(currency + "_cur")) out.println(newOrig);
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
    	// btc to usd is low volume (outdated) so get btc to cad and convert to usd
        String strURL = "https://api.quadrigacx.com/v2/ticker?book=btc_cad";
        URL btcURL = new URL(strURL);
        URLConnection btcURLConnection = btcURL.openConnection();
        //server returns 403 if user agent is not set
        btcURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
        BigDecimal cadPrice;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(btcURLConnection.getInputStream()))) {
            Gson gson = new Gson(); // parse JSON reply
            Price btcPrice = gson.fromJson(in.readLine(), Price.class);
            cadPrice = btcPrice.last; // price in CAD
        }
        strURL = "https://download.finance.yahoo.com/d/quotes.csv?s=CADUSD=X&f=l1"; // get last trade price of cad to usd
        URL yahooURL = new URL(strURL);
        BigDecimal cadToUsd;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(yahooURL.openStream()))) {
        	cadToUsd = new BigDecimal(in.readLine());
        }
        BigDecimal usdPrice = cadPrice.multiply(cadToUsd).setScale(2, RoundingMode.UP);
        return usdPrice.toString();
    }
    private String getEthPrice() throws Exception {
        // can't get usd price directly from quadrigacx.com so have to get cad price then convert
        String strURL = "https://api.quadrigacx.com/v2/ticker?book=eth_cad";
        URL ethURL = new URL(strURL);
        URLConnection ethURLConnection = ethURL.openConnection();
        // server returns 403 if user agent is not set
        ethURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
        BigDecimal cadPrice;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(ethURLConnection.getInputStream()))) {
            Gson gson = new Gson(); // parse JSON reply
            Price price = gson.fromJson(in.readLine(), Price.class);
            cadPrice = price.last;
        }
        strURL = "https://download.finance.yahoo.com/d/quotes.csv?s=CADUSD=X&f=l1"; // get last trade price of cad to usd
        URL yahooURL = new URL(strURL);
        BigDecimal cadToUsd;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(yahooURL.openStream()))) {
            cadToUsd = new BigDecimal(in.readLine());
        }
        BigDecimal usdPrice = cadPrice.multiply(cadToUsd).setScale(2, RoundingMode.UP);
        return usdPrice.toString();
    }
    private String getCurrencyExchangeRate(String cur) throws Exception {
        boolean hasPrecedence; // if currency has conventional precedence
        String strURL;
        if (cur.equalsIgnoreCase("eur") || cur.equalsIgnoreCase("gbp") || cur.equalsIgnoreCase("aud") ||
                cur.equalsIgnoreCase("nzd")) {
            hasPrecedence = true;
            strURL = "https://download.finance.yahoo.com/d/quotes.csv?s=" + cur + "usd=X&f=l1";
        }
        else {
            hasPrecedence = false;
            strURL = "https://download.finance.yahoo.com/d/quotes.csv?s=usd" + cur + "=X&f=l1"; // get inverse rate
        }
        URL currencyURL = new URL(strURL);
        String rate;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(currencyURL.openStream()))) {
            rate = in.readLine();
        }
        if (hasPrecedence) return rate;
        else {
            BigDecimal realRate = new BigDecimal("1").divide(new BigDecimal(rate), 4, RoundingMode.HALF_UP); // invert
            return realRate.toString();
        }

    }
}
