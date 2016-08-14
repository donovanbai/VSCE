import java.net.*;
import java.io.*;
import java.util.*;

public class Test {
    public static void main(String[] args) throws Exception {
        String strURL = "https://download.finance.yahoo.com/d/quotes.csv?s="; //base URL
        //create ArrayList to hold stocks
        ArrayList<String> stockList = new ArrayList<>();
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        //prompt user for stocks, append them to URL, add them to ArrayList
        System.out.println("enter stocks");
        String stock = stdIn.readLine();
        while (stock.equals("") == false) { //as long as user enters non-empty line
            strURL += stock + "+";
            stockList.add(stock);
            stock = stdIn.readLine();
        }
        strURL = strURL.substring(0, strURL.length()-1); //remove the last "+" from URL
        strURL += "&f=ab";
        URL myURL = new URL(strURL);
        //print out stock prices which are returned as CSV
        BufferedReader in = new BufferedReader(new InputStreamReader(myURL.openStream()));
        String inputLine;
        int i = 0;
        while ((inputLine = in.readLine()) != null) {           
            System.out.print(stockList.get(i) + ": ");
            System.out.println(inputLine);
            i++;
        }
        in.close();        
    }
}
