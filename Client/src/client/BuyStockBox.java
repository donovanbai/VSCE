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
    PrintWriter out;
    BufferedReader in;
    String stock, username;
    TextField qField = new TextField();
    Text msg = new Text();
    Text balText, homeText;

    public void display(String stock, double price, double bal, Text homeText, String username, PrintWriter out, BufferedReader in) {
        this.out = out;
        this.in = in;
        this.stock = stock;
        this.homeText = homeText;
        this.username = username;
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Buy " + stock);
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        
        String s = String.format("%s $%,.2f", "your balance:", bal);
        balText = new Text(s);
        grid.add(balText, 1, 0);
        
        Label quantity = new Label("quantity");
        grid.add(quantity, 0, 1);
        
        qField.setOnAction(e -> {
            
        });
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
        
        grid.add(msg, 1, 4);
        
        buyBtn.setOnAction(e -> {
            buyStock();
        });
        
        Scene scene = new Scene(grid, 300, 200);
        window.setScene(scene);
        window.showAndWait();
    }
    
    public void buyStock() {
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
        if (serverReply.equals("fail")) {
            msg.setFill(Color.RED);
            msg.setText("stock price could not be retrieved");
        }
        else if (serverReply.equals("1")) {
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
                String s3 = String.format("%s %s %s $%,.2f%s", "logged in as", username, "(balance:", newBal, ")");
                homeText.setText(s3);
            } catch (IOException i) {
                System.out.println("IOException while getting server reply");
            }
        }
    }
}
