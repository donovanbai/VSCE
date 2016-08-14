//go to http://www.jarloo.com/yahoo_finance/ for api spec

import java.net.*;
import java.io.*;
import java.util.*;

public class TimerTest {
    public static void main(String[] args) throws Exception {
        String strURL = "https://download.finance.yahoo.com/d/quotes.csv?s="; //base URL
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        //prompt user for stocks, append them to URL
        System.out.println("enter stocks");
        String stock = stdIn.readLine();
        while (stock.equals("") == false) { //as long as user enters non-empty line
            strURL += stock + "+";
            stock = stdIn.readLine();
        }
        strURL = strURL.substring(0, strURL.length()-1); //remove the last "+" from URL
        strURL += "&f=sl1"; //s = symbol and l1 = last trade price.
        URL myURL = new URL(strURL);
		//scheduled task timer
		int delay = 0;
		int period = 10000; //repeats every 10 seconds
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
				try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(myURL.openStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {           
                        System.out.println(inputLine);
                    }
                    System.out.println();
                    in.close();	
                } catch (MalformedURLException e) {
                    System.err.println("MalformedURLException thrown");
				} catch (IOException e) {
                    System.err.println("IOException thrown");
				}
            }
		}, delay, period);
    }
}


