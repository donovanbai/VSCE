//go to https://www.quadrigacx.com/api_info for api documentation

import java.net.*;
import java.io.*;
import java.util.*;

public class QcxTest {
    public static void main(String[] args) throws Exception {
        String strURL = "https://api.quadrigacx.com/v2/ticker?book="; //base URL
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		//prompt user if they want to get data for bitcoin or ether
        System.out.println("enter currency (btc or eth)");
        String curr = stdIn.readLine();
		strURL += curr + "_cad";
        URL myURL = new URL(strURL);

		//retrieve info every 10 seconds
		int delay = 0;
		int period = 10000; 
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
				try {
					URLConnection myURLConnection = myURL.openConnection();
					//server returns 403 if user agent is not set
					myURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    BufferedReader in = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
					String inputLine;
					while ((inputLine = in.readLine()) != null) {
						System.out.println(inputLine); //data is returned in JSON format
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
