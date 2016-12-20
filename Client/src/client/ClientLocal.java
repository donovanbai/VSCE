package client;

import java.io.*;
import java.math.BigDecimal;
import java.net.*;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.text.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
    static Scene login, home, profile;
    static Text loginText = new Text(); 
    static Text homeText = new Text();
    static Text homeText2 = new Text();
    static String username, pw;
    static Button buyBtn = new Button("Buy");
    
    static String stock, currency;
    static BigDecimal price;
    static BigDecimalWrapper bal;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        setupLogin();

        window = primaryStage;
        window.setTitle("VSCE");
        window.setScene(login);        
        window.show();
    }
    
    public static void setupLogin() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(25, 25, 25, 25)); //top, right, bottom, left
        grid.setHgap(15);
        grid.setVgap(15);

        Text scenetitle = new Text("Welcome to VSCE\n");
        scenetitle.setFont(Font.font("Calibri", FontWeight.BOLD, 30));
        grid.add(scenetitle, 0, 0); //column, row
        GridPane.setHalignment(scenetitle, HPos.CENTER);

        Label userLabel = new Label("Username:");
        userLabel.setFont(Font.font("Calibri", 20));
        TextField userField = new TextField();
        userField.setFont(Font.font("Calibri", 20));
        HBox hbox = new HBox(15);
        hbox.getChildren().addAll(userLabel, userField);
        grid.add(hbox, 0, 1);
        GridPane.setHalignment(hbox, HPos.CENTER);

        Label pwLabel = new Label("Password:");
        pwLabel.setFont(Font.font("Calibri", 20));
        PasswordField pwField = new PasswordField();
        pwField.setFont(Font.font("Calibri", 20));
        HBox hbox2 = new HBox(15);
        hbox2.getChildren().addAll(pwLabel, pwField);
        grid.add(hbox2, 0, 2);
        GridPane.setHalignment(hbox2, HPos.CENTER);
        
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
        
        loginText.setFont(Font.font("Calibri", 20));
        grid.add(loginText, 0, 6);
        GridPane.setHalignment(loginText, HPos.CENTER);

        Button signInBtn = new Button("Sign in");
        signInBtn.setFont(Font.font("Calibri", 20));
        signInBtn.setOnAction(e -> {
            username = userField.getText();
            pw = pwField.getText();
            login();
        });
        
        Button registerBtn = new Button("Register");
        registerBtn.setFont(Font.font("Calibri", 20));
        registerBtn.setOnAction(e -> {
            username = userField.getText();
            pw = pwField.getText();
            register();
        });

        HBox hbButton = new HBox(20);
        hbButton.setAlignment(Pos.BOTTOM_RIGHT);
        hbButton.getChildren().addAll(signInBtn, registerBtn);
        grid.add(hbButton, 0, 4);
        GridPane.setHalignment(hbButton, HPos.CENTER);

        //grid.setGridLinesVisible(true);

        login = new Scene(grid, 500, 500);       
    }
    
    public static void setupHome() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(25, 25, 25, 25)); //top, right, bottom, left
        grid.setHgap(15);
        grid.setVgap(15);
        
        homeText.setFont(Font.font("Calibri", 30));
        grid.add(homeText, 0, 0); 
        GridPane.setHalignment(homeText, HPos.CENTER);
        
        Button profBtn = new Button("View profile");
        profBtn.setFont(Font.font("Calibri", 20));
        profBtn.setOnAction(e -> {
            setupProfile();
        });        
        grid.add(profBtn, 0, 2);
        GridPane.setHalignment(profBtn, HPos.CENTER);
        
        Label stockLabel = new Label("search for a stock (eg. aapl)");
        stockLabel.setFont(Font.font("Calibri", 20));
        
        TextField stockField = new TextField();
        stockField.setFont(Font.font("Calibri", 20));
        stockField.setOnAction(e -> {
            stock = stockField.getText();
            searchStock();  
        });
        
        Button stockBtn = new Button("search");
        stockBtn.setFont(Font.font("Calibri", 20));
        stockBtn.setOnAction(e -> {
            stock = stockField.getText();
            searchStock();
        });
        
        HBox hbox = new HBox(15);
        hbox.getChildren().addAll(stockLabel, stockField, stockBtn);
        grid.add(hbox, 0, 5);
        GridPane.setHalignment(hbox, HPos.CENTER);
           
        Label currencyLabel = new Label("search for a currency pair\n(eg. usd/cad)");
        currencyLabel.setFont(Font.font("Calibri", 20));
        
        TextField currencyField = new TextField();
        currencyField.setFont(Font.font("Calibri", 20));
        currencyField.setOnAction(e -> {
            currency = currencyField.getText();
            searchCurrency();
        });
        
        Button currencyBtn = new Button("search");
        currencyBtn.setFont(Font.font("Calibri", 20));
        currencyBtn.setOnAction(e -> {
            currency = currencyField.getText();
            searchCurrency();
        });
        
        HBox hbox2 = new HBox(15);
        hbox2.getChildren().addAll(currencyLabel, currencyField, currencyBtn);
        grid.add(hbox2, 0, 6);
        GridPane.setHalignment(hbox2, HPos.CENTER);
        
        buyBtn.setVisible(false);
        buyBtn.setFont(Font.font("Calibri", 20));
        buyBtn.setOnAction(e -> {
            BuyStockBox box = new BuyStockBox();
            box.display(stock, price, bal, homeText, username, out, in);
        });
        HBox hbox4 = new HBox(15);
        homeText2.setFont(Font.font("Calibri", 20));
        hbox4.getChildren().addAll(homeText2, buyBtn);   
        grid.add(hbox4, 0, 8);
        GridPane.setHalignment(hbox4, HPos.CENTER);
        GridPane.setFillWidth(hbox4, false); //otherwise hbox2 would be too wide
        
        Button logoutBtn = new Button("Logout");
        logoutBtn.setFont(Font.font("Calibri", 20));
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
        grid.add(logoutBtn, 0, 9);
        GridPane.setHalignment(logoutBtn, HPos.RIGHT);
      
        //grid.setGridLinesVisible(true);
        home = new Scene(grid, 700, 500);
    }
    
    public static void setupProfile() {
        //show loading screen while fetching data from server
        Text text = new Text("loading...");
        VBox vbox = new VBox(text);
        profile = new Scene(vbox, 500, 500);
        window.setScene(profile);
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(25, 25, 25, 25)); //top, right, bottom, left
        grid.setHgap(15);
        grid.setVgap(15);
        
        String style = "-fx-alignment: CENTER; -fx-font-family: Calibri; -fx-font-size: 20";
        
        TableColumn<Asset, String> nameCol = new TableColumn<>("Name");
        //nameCol.setMinWidth(200);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setStyle(style); //set text alignment in column
        
        TableColumn<Asset, String> typeCol = new TableColumn<>("Type");
        //nameCol.setMinWidth(200);
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setStyle(style);
        
        TableColumn<Asset, Double> priceCol = new TableColumn<>("Price");
        //nameCol.setMinWidth(200);
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setStyle(style);
        
        TableColumn<Asset, Integer> quantityCol = new TableColumn<>("Quantity");
        //nameCol.setMinWidth(200);
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setStyle(style);
        
        TableColumn<Asset, Double> totalValCol = new TableColumn<>("Total Value");
        //nameCol.setMinWidth(200);
        totalValCol.setCellValueFactory(new PropertyValueFactory<>("totalVal"));
        totalValCol.setStyle(style);
        
        TableView<Asset> table = new TableView<>();
        table.setMinWidth(600);
        //table.setStyle(style);
        ObservableList<Asset> assets = FXCollections.observableArrayList();
        //table.setItems(assets);
        table.getColumns().addAll(nameCol, typeCol, priceCol, quantityCol, totalValCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); //prevent an extra column from being created
        //grid.add(table, 0, 0);
    
        Task task = new Task<Void>() {
            @Override
            protected Void call() {
                out.println("get profile");               
                try {
                    String serverReply = in.readLine();
                    while (!serverReply.equals("end")) {
                        String[] arr = serverReply.split("_"); //eg. parse "aapl_stock" into "appl" and "stock"
                        String name = arr[0];
                        String type = arr[1];
                        int quantity = Integer.parseInt(in.readLine());
                        BigDecimal price = new BigDecimal(in.readLine());      
                        assets.add(new Asset(name, type, price, quantity));
                        serverReply = in.readLine();
                    }
                } catch (IOException e) {
                    System.out.println("IOException while getting server reply");
                }
                return null;
            }
        };
        
        new Thread(task).start();
        task.setOnSucceeded(e -> {
            table.setItems(assets);
            grid.add(table, 0, 0);
            Button homeBtn = new Button("Home");
            homeBtn.setFont(Font.font("Calibri", 20));
            homeBtn.setOnAction(e2 -> {
                window.setScene(home);
            });
            grid.add(homeBtn, 0, 1);
            GridPane.setHalignment(homeBtn, HPos.RIGHT);
            profile = new Scene(grid, 700, 500);
            window.setScene(profile);
        }); 
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
                        bal = new BigDecimalWrapper(new BigDecimal(in.readLine()));
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
                String text = String.format("%s %s %s $%,.2f%s", "logged in as", username, "(balance:", bal.bd, ")");               
                homeText.setText(text);
                setupHome();
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
    
    public static void searchStock() {
        //price = 0;
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
                if (serverReply.equals("fail")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("failed to retrive price");
                }
                else if (serverReply.equals("N/A")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("unknown stock symbol");
                }
                else {
                    price = new BigDecimal(serverReply);
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
            buyBtn.visibleProperty().unbind();
        });
    }
    
    public static void searchCurrency() {
        //price = 0;
        BooleanProperty showBuyBtn = new SimpleBooleanProperty(false);
        buyBtn.visibleProperty().bind(showBuyBtn);
        ObjectProperty textColorProperty = new SimpleObjectProperty();
        Task task = new Task<Void>() {
            @Override
            protected Void call() {              
                if (currency.equals("")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("field cannot be empty");
                    return null;
                }
                out.println("search currency");
                out.println(currency);
                String serverReply; // NEED TO WRITE SERVER SIDE CODE
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("IOException while getting server reply");
                    return null;
                }
                if (serverReply.equals("fail")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("failed to retrive price");
                }
                else if (serverReply.equals("N/A")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("unknown currency");
                }
                else {
                    price = new BigDecimal(serverReply);
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
            buyBtn.visibleProperty().unbind();
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}