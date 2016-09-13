package client;

import java.io.*;
import java.net.*;

import javafx.application.Application;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.text.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
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
    static Text loginText = new Text(); 
    static Text homeText = new Text();
    static Text homeText2 = new Text();
    static String username, pw;
    static Button buyBtn = new Button("Buy");
    
    static String  stock;
    static double bal, price;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        setupLogin();
        setupHome();

        window = primaryStage;
        window.setTitle("VSCE");
        window.setScene(login);        
        window.show();
    }

    public static void login() {
        if (username.equals("") || pw.equals("")) {
            loginText.setFill(Color.RED);
            loginText.setText("username and password cannot be empty");
            return;
        }
        ObjectProperty textColorProperty = new SimpleObjectProperty();
        Task task = new Task<Integer>() {
            @Override
            protected Integer call() {
                textColorProperty.setValue(Color.GREEN);
                updateMessage("connecting...");
                try {
                    hostIP = InetAddress.getLocalHost();
                    socket = new Socket(hostIP, portNumber);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (Exception e) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("unable to connect to server");
                    return 1;
                }
                out.println("sign in");
                out.println(username);
                out.println(pw);
                String serverReply;
                try {
                    serverReply = in.readLine();
                } catch (IOException e) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("IOException while getting server reply");
                    return 1;
                }
                if (serverReply.equals("0")) {
                    try {
                        bal = Double.parseDouble(in.readLine());
                    } catch (IOException e) {
                        textColorProperty.setValue(Color.RED);
                        updateMessage("IOException while getting server reply");
                        return 1;
                    }
                    return 0;
                }
                else if (serverReply.equals("1")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("username does not exist");
                    return 1;
                }
                else if (serverReply.equals("2")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("incorrect password");
                    return 1;
                }
                else {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("invalid server reply");
                    return 1;
                }
            }
        };
        
        loginText.fillProperty().bind(textColorProperty);
        loginText.textProperty().bind(task.messageProperty());
        new Thread(task).start();
        task.setOnSucceeded(e -> {
            int result = (int) task.getValue();
            if (result == 0) {
                String text = String.format("%s %s %s $%,.2f%s", "logged in as", username, "(balance:", bal, ")");
                homeText.setText(text);
                window.setScene(home);
            }
            loginText.textProperty().unbind();
            loginText.fillProperty().unbind();
        });
    }
    
    public static void register() {
        if (username.equals("") || pw.equals("")) {
            loginText.setFill(Color.RED);
            loginText.setText("username and password cannot be empty");
            return;
        }
        ObjectProperty textColorProperty = new SimpleObjectProperty();
        Task task = new Task<Void>() {
            @Override
            protected Void call() {
                textColorProperty.setValue(Color.GREEN);
                updateMessage("connecting...");
                try {
                    hostIP = InetAddress.getLocalHost();
                    socket = new Socket(hostIP, portNumber);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (Exception e) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("unable to connect to server");
                    return null;
                }
                out.println("register");
                out.println(username);
                out.println(pw);
                String serverReply = null;
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("IOException while getting server reply");
                    return null;
                }
                if (serverReply.equals("0")) {
                    textColorProperty.setValue(Color.GREEN);
                    updateMessage("registration successful!");
                    return null;
                }
                else if (serverReply.equals("1")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("username already taken");
                    return null;
                }
                else {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("invalid server reply");
                    return null;
                }
            }
        };
        loginText.fillProperty().bind(textColorProperty);
        loginText.textProperty().bind(task.messageProperty());
        new Thread(task).start();
        task.setOnSucceeded(e -> {
            loginText.textProperty().unbind();
            loginText.fillProperty().unbind();
            try {
                socket.close();
            } catch (IOException i) {
                System.out.println("IOException while closing socket and input stream");
            }
        });
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
        
        userField.setOnAction(e -> {
            username = userField.getText();
            pw = pwField.getText();
            login();           
        });
        
        pwField.setOnAction(e -> {
            username = userField.getText();
            pw = pwField.getText();
            login(); 
        });
        
        grid.add(loginText, 1, 6);

        Button signInBtn = new Button("Sign in");
        signInBtn.setOnAction(e -> {
            username = userField.getText();
            pw = pwField.getText();
            login();
        });
        
        Button registerBtn = new Button("Register");
        registerBtn.setOnAction(e -> {
            username = userField.getText();
            pw = pwField.getText();
            register();
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
        stockField.setOnAction(e -> {
            stock = stockField.getText();
            searchStock();  
        });
        grid.add(stockField, 1, 5);
        
        Button stockBtn = new Button("search");
        stockBtn.setOnAction(e -> {
            stock = stockField.getText();
            searchStock();
        });
        grid.add(stockBtn, 2, 5);
        
        buyBtn.setOnAction(e -> {
            BuyStockBox box = new BuyStockBox();
            box.display(stock, price, bal, homeText, username, out, in);
        });
        buyBtn.setVisible(false);
        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(homeText2, buyBtn);       
        grid.add(hbox, 1, 6);
        
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> {
            loginText.setText("");
            window.setScene(login);
            out.println("logout");
            try {
                socket.close();
            } catch (IOException i) {
                System.out.println(i.getMessage());
            }
        });
        grid.add(logoutBtn, 2, 7);
      
        home = new Scene(grid, 500, 500);
    }
    
    public static void searchStock() {
        price = 0;
        BooleanProperty showBuyBtn = new SimpleBooleanProperty(false);
        buyBtn.visibleProperty().bind(showBuyBtn);
        ObjectProperty textColorProperty = new SimpleObjectProperty();
        Task task = new Task<Void>() {
            @Override
            protected Void call() {              
                if (stock.equals("")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("field cannot be empty");
                    return null;
                }
                out.println("search stock");
                out.println(stock);
                String serverReply;
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("IOException while getting server reply");
                    return null;
                }
                if (serverReply.equals("N/A")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("unknown stock symbol");
                }
                else {
                    price = Double.parseDouble(serverReply);
                    textColorProperty.setValue(Color.GREEN);
                    updateMessage("last trade: $" + serverReply);
                    showBuyBtn.setValue(true);
                }
                return null;
            }
        };
        
        homeText2.fillProperty().bind(textColorProperty);
        homeText2.textProperty().bind(task.messageProperty());       
        new Thread(task).start(); 
        task.setOnSucceeded(e -> {
            homeText2.textProperty().unbind();
            homeText2.fillProperty().unbind();
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
