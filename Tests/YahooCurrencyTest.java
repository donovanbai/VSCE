//go to http://www.jarloo.com/yahoo_finance/ for api spec

import java.net.*;
import java.io.*;
import java.util.*;

public class YahooCurrencyTest {
    public static void main(String[] args) throws Exception {
        String strURL = "https://download.finance.yahoo.com/d/quotes.csv?s="; //base URL
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		//prompt user for currency pairs and append to URL
        System.out.println("enter currency pairs (eg. USDCAD for USD to CAD exchange rate) separated by newlines");
		String curPair = stdIn.readLine();
        while (curPair.equals("") == false) { //as long as user enters non-empty line
            strURL += curPair + "=X+";
            curPair = stdIn.readLine();
        }
        strURL = strURL.substring(0, strURL.length()-1); //remove the last "+" from URL
        strURL += "&f=sl1"; //s = symbol and l1 = last trade price.
        URL myURL = new URL(strURL);

		//retrieve info every 10 seconds
		int delay = 0;
		int period = 10000; 
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
				try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(myURL.openStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {           
                        System.out.println(inputLine); //data is returned in csv format
                    }
                    System.out.println();
                    in.close();	
                } catch (MalformedURLException e) {
                    System.err.println(e.getMessage());
				} catch (IOException e) {
                    System.err.println(e.getMessage());
				}
            }
		}, delay, period);
    }
}


