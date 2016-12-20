package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Locale;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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

    public void display(String stock, BigDecimal price, BigDecimalWrapper bal, Text homeText, String username, PrintWriter out, BufferedReader in) {
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
        grid.setHgap(15);
        grid.setVgap(15);
        
        String s = String.format("%s $%,.2f\n", "your balance:", bal.bd);
        balText = new Text(s);
        balText.setFont(Font.font("Calibri", 20));
        grid.add(balText, 0, 0);
        GridPane.setHalignment(balText, HPos.CENTER);
        
        Label quantity = new Label("quantity");
        quantity.setFont(Font.font("Calibri", 20));
        
        qField.setFont(Font.font("Calibri", 20));
        qField.setOnAction(e -> {
            buyStock(bal);
        });
        
        HBox hbox = new HBox(15);
        hbox.getChildren().addAll(quantity, qField);
        grid.add(hbox, 0, 1);
        GridPane.setHalignment(hbox, HPos.CENTER);

        Text costText = new Text();
        
        IntegerProperty ip = new SimpleIntegerProperty();
        StringConverter<Number> converter = new NumberStringConverter();
        Bindings.bindBidirectional(qField.textProperty(), ip, converter);    
        Locale locale = Locale.CANADA;
        costText.textProperty().bind(Bindings.format(locale, "estimated cost: $%,.2f", ip.multiply(price.doubleValue())));
        
        costText.setFont(Font.font("Calibri", 20));
        costText.setFill(Color.GREEN);
        grid.add(costText, 0, 2);
        GridPane.setHalignment(costText, HPos.CENTER);
        
        Button buyBtn = new Button("Buy now");
        buyBtn.setFont(Font.font("Calibri", 20));
        buyBtn.setOnAction(e -> {
            buyStock(bal);
        });
        grid.add(buyBtn, 0, 3);
        GridPane.setHalignment(buyBtn, HPos.CENTER);
        
        msg.setFont(Font.font("Calibri", 20));
        grid.add(msg, 0, 5);
        GridPane.setHalignment(msg, HPos.CENTER);
          
        Scene scene = new Scene(grid, 400, 300);
        window.setScene(scene);
        window.showAndWait();
    }
    
    public void buyStock(BigDecimalWrapper bal) {
        try {
            Integer.parseInt(qField.getText());
        } catch (NumberFormatException e) {
            msg.setFill(Color.RED);
            msg.setText("invalid input");
            return;
        }
        if (Integer.parseInt(qField.getText()) < 1) {
            msg.setFill(Color.RED);
            msg.setText("quantity has to be > 0");
            return;
        }
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
                BigDecimal newBal = new BigDecimal(in.readLine());
                String s2 = String.format("%s $%,.2f", "your balance:", newBal);
                balText.setText(s2);
                String s3 = String.format("%s %s %s $%,.2f%s", "logged in as", username, "(balance:", newBal, ")");
                homeText.setText(s3);
                bal.bd = newBal;
            } catch (IOException i) {
                System.out.println("IOException while getting server reply");
            }
        }
    }
}
