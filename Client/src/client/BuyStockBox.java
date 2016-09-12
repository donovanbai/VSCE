package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

public class BuyStockBox {

    public void display(String stock, double price, double bal, PrintWriter out, BufferedReader in) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Buy " + stock);
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        
        String s = String.format("%s $%,.2f", "your balance:", bal);
        Text balText = new Text(s);
        grid.add(balText, 1, 0);
        
        Label quantity = new Label("quantity");
        grid.add(quantity, 0, 1);
        
        TextField qField = new TextField();
        grid.add(qField, 1, 1);

        Text costText = new Text();
        
        IntegerProperty ip = new SimpleIntegerProperty();
        StringConverter<Number> converter = new NumberStringConverter();
        Bindings.bindBidirectional(qField.textProperty(), ip, converter);    
        Locale locale = Locale.CANADA;
        costText.textProperty().bind(Bindings.format(locale, "estimated cost: $%,.2f", ip.multiply(price)));
        
        costText.setFill(Color.GREEN);
        grid.add(costText, 1, 2);
        
        Button buyBtn = new Button("Buy now");
        grid.add(buyBtn, 1, 3);
        
        Text msg = new Text();
        grid.add(msg, 1, 4);
        
        buyBtn.setOnAction(e -> {
            msg.setFill(Color.GREEN);
            msg.setText("loading...");
            out.println("buy stock");
            out.println(stock);
            out.println(qField.getText());
            String serverReply = null;
            try {
                serverReply = in.readLine();
            } catch (IOException i) {
                System.out.println("IOException while getting server reply");
            }
            if (serverReply.equals("1")) {
                msg.setFill(Color.RED);
                msg.setText("insufficient balance!");
            }
            else if (serverReply.equals("0")) {
                msg.setFill(Color.GREEN);
                msg.setText("purchase complete!");
                try {
                    Double newBal = Double.parseDouble(in.readLine());
                    String s2 = String.format("%s $%,.2f", "your balance:", newBal);
                    balText.setText(s2);
                } catch (IOException i) {
                    System.out.println("IOException while getting server reply");
                }
            }
        });
        
        Scene scene = new Scene(grid, 300, 200);
        window.setScene(scene);
        window.showAndWait();
    }
    
    /*public void buyStock() {
        Task task = new Task<Integer>() {
            @Override
            protected Integer call() {     
                updateMessage("connecting...");
                try {
                    hostIP = InetAddress.getLocalHost();
                    socket = new Socket(hostIP, portNumber);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (Exception e) {
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
                    updateMessage("IOException while getting server reply");
                    return 1;
                }
                if (serverReply.equals("0")) {
                    try {
                        bal = Double.parseDouble(in.readLine());
                    } catch (IOException e) {
                        updateMessage("IOException while getting server reply");
                        return 1;
                    }
                    return 0;
                }
                else if (serverReply.equals("1")) {
                    updateMessage("username does not exist");
                    return 1;
                }
                else if (serverReply.equals("2")) {
                    updateMessage("incorrect password");
                    return 1;
                }
                else {
                    updateMessage("invalid server reply");
                    return 1;
                }
            }
        };
        
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
        });
    }*/
}
