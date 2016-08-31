package client;

import java.io.*;
import java.net.*;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.text.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
 
public class ClientLocal extends Application {
    
    static int portNumber = 10000;
    static InetAddress hostIP;
    static Socket socket;
    static PrintWriter out;
    static BufferedReader in;
    static BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
    
    static Stage window;
    static Scene login, home;
    static Text homeText = new Text();
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        initVars();
        setupLogin();
        setupHome();

        window = primaryStage;
        window.setTitle("VSCE");
        window.setScene(login);        
        window.show();
    }

    public static void initVars() throws Exception {
        hostIP = InetAddress.getLocalHost();
        socket = new Socket(hostIP, portNumber);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    
    public static void setupLogin() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25)); //top, right, bottom, left

        Text scenetitle = new Text("Welcome to VSCE");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(scenetitle, 0, 0, 2, 1); //column, row, column span, row span

        Label userLabel = new Label("Username:");
        grid.add(userLabel, 0, 1);

        TextField userField = new TextField();
        grid.add(userField, 1, 1);

        Label pwLabel = new Label("Password:");
        grid.add(pwLabel, 0, 2);

        PasswordField pwField = new PasswordField();
        grid.add(pwField, 1, 2);
        
        Text msg = new Text();
        grid.add(msg, 1, 6);

        Button signInBtn = new Button("Sign in");
        signInBtn.setOnAction(e -> {
            String username = userField.getText();
            String pw = pwField.getText();
            
            if (username.equals("") || pw.equals("")) {
                msg.setFill(Color.RED);
                msg.setText("username and password cannot be empty");
            }
            else {
                out.println("sign in");
                out.println(username);
                out.println(pw);
                String serverReply = null;
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {
                    System.out.println("IOException while getting server reply");
                }
                if (serverReply.equals("0")) {
                    String bal = null;
                    try {
                        bal = in.readLine();
                    } catch (IOException i) {
                        System.out.println(i.getMessage());
                    }
                    homeText.setText("logged in as " + username + " (balance: " + bal + ")");
                    window.setScene(home);
                }
                else if (serverReply.equals("1")) {
                    msg.setFill(Color.RED);
                    msg.setText("username does not exist");
                }
                else if (serverReply.equals("2")) {
                    msg.setFill(Color.RED);
                    msg.setText("incorrect password");
                }
            }
        });
        
        Button registerBtn = new Button("Register");
        registerBtn.setOnAction(e -> {
            String username = userField.getText();
            String pw = pwField.getText();
            
            if (username.equals("") || pw.equals("")) {
                msg.setFill(Color.RED);
                msg.setText("username and password cannot be empty");
            }
            else {
                out.println("register");
                out.println(userField.getText());
                out.println(pwField.getText());

                String serverReply = null;
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {
                    System.out.println("IOException while getting server reply");
                }
                if (serverReply.equals("0")) {
                    msg.setFill(Color.GREEN);
                    msg.setText("registration successful!");
                }
                else if (serverReply.equals("1")) {
                    msg.setFill(Color.RED);
                    msg.setText("username already taken");
                }
            }
        });

        HBox hbButton = new HBox(20);
        hbButton.setAlignment(Pos.BOTTOM_RIGHT);
        hbButton.getChildren().addAll(signInBtn, registerBtn);
        grid.add(hbButton, 0, 4, 2, 1);

        //grid.setGridLinesVisible(true);

        login = new Scene(grid, 500, 500);       
    }
    
    public static void setupHome() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        //grid.setPadding(new Insets(25, 25, 25, 25));
        
        homeText.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(homeText, 0, 0, 2, 1); 
        
        Label stockLabel = new Label("search for a stock");
        grid.add(stockLabel, 0, 5);
        
        TextField stockField = new TextField();
        grid.add(stockField, 1, 5);
        
        Text msg = new Text();
        grid.add(msg, 0, 6);
        
        Button stockBtn = new Button("search");
        stockBtn.setOnAction(e -> {
            String stock = stockField.getText();
            
            if (stock.equals("")) {
                msg.setFill(Color.RED);
                msg.setText("field cannot be empty");
            }
            else {
                out.println("search stock");
                out.println(stock);
                String serverReply = null;
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {
                    System.out.println("IOException while getting server reply");
                }
                if (serverReply.equals("N/A")) {
                    msg.setFill(Color.RED);
                    msg.setText("unknown stock symbol");
                }
                else {
                    msg.setFill(Color.GREEN);
                    msg.setText("last trade: " + serverReply);
                }
            }
        });
        grid.add(stockBtn, 2, 5);
        
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> {
            window.setScene(login);
            out.println("logout");
        });
        grid.add(logoutBtn, 2, 7);
      
        home = new Scene(grid, 500, 500);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
