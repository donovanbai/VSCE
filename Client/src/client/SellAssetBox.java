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
import javafx.scene.control.TableView;
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

public class SellAssetBox {
    PrintWriter out;
    BufferedReader in;
    String stock, username;
    TextField qField = new TextField();
    Text msg = new Text();
    Text balText, homeText, qText;
    int quantityOwned, row;
    BigDecimalWrapper bal;
    TableView<Asset> table;
    
    public void display(String stock, BigDecimal price, BigDecimalWrapper bal, int quantityOwned, Text homeText, String username, TableView<Asset> table, int row, PrintWriter out, BufferedReader in) {
        this.stock = stock;
        this.homeText = homeText;
        this.username = username;
        this.table = table;
        this.row = row;
        this.out = out;
        this.in = in;
        this.quantityOwned = quantityOwned;
        this.bal = bal;
        
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Sell " + stock);
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(15);
        grid.setVgap(15);
        
        String s = String.format("%s $%,.2f", "your balance:", bal.bd);
        balText = new Text(s);
        balText.setFont(Font.font("Calibri", 20));
        grid.add(balText, 0, 0);
        GridPane.setHalignment(balText, HPos.CENTER);
        
        String s2 = "shares owned: " + quantityOwned + "\n";
        qText = new Text(s2);
        qText.setFont(Font.font("Calibri", 20));
        grid.add(qText, 0, 1);
        GridPane.setHalignment(qText, HPos.CENTER);
        
        Label quantity = new Label("quantity");
        quantity.setFont(Font.font("Calibri", 20));
        
        qField.setFont(Font.font("Calibri", 20));
        qField.setOnAction(e -> {
            sellStock();
        });
        
        HBox hbox = new HBox(15);
        hbox.getChildren().addAll(quantity, qField);
        grid.add(hbox, 0, 2);
        GridPane.setHalignment(hbox, HPos.CENTER);

        Text totalText = new Text();
        
        IntegerProperty ip = new SimpleIntegerProperty();
        StringConverter<Number> converter = new NumberStringConverter();
        Bindings.bindBidirectional(qField.textProperty(), ip, converter);    
        Locale locale = Locale.CANADA;
        totalText.textProperty().bind(Bindings.format(locale, "estimated total: $%,.2f", ip.multiply(price.doubleValue())));
        
        totalText.setFont(Font.font("Calibri", 20));
        totalText.setFill(Color.GREEN);
        grid.add(totalText, 0, 3);
        GridPane.setHalignment(totalText, HPos.CENTER);
        
        Button sellBtn = new Button("Sell now");
        sellBtn.setFont(Font.font("Calibri", 20));
        sellBtn.setOnAction(e -> {
            sellStock();
        });
        grid.add(sellBtn, 0, 4);
        GridPane.setHalignment(sellBtn, HPos.CENTER);
        
        msg.setFont(Font.font("Calibri", 20));
        grid.add(msg, 0, 6);
        GridPane.setHalignment(msg, HPos.CENTER);
          
        Scene scene = new Scene(grid, 400, 300);
        window.setScene(scene);
        window.showAndWait();
    }
    
    private void sellStock() {
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
        if (Integer.parseInt(qField.getText()) > quantityOwned) {
            msg.setFill(Color.RED);
            msg.setText("you do not own that many shares");
            return;
        }
        msg.setFill(Color.GREEN);
        msg.setText("loading...");
        out.println("sell stock");
        out.println(stock);
        out.println(qField.getText());
        String serverReply = null;
        try {
            serverReply = in.readLine();
        } catch (IOException i) {
            System.out.println("IOException while getting server reply");
        }
        if (serverReply.equals("fail") || serverReply.equals("N/A")) {
            msg.setFill(Color.RED);
            msg.setText("stock price could not be retrieved");
        }
        else if (serverReply.equals("1")) {
            msg.setFill(Color.RED);
            msg.setText("you do not own that many shares. re-login.");
        }
        else if (serverReply.equals("0")) {
            msg.setFill(Color.GREEN);
            msg.setText("sale complete!"); 
            try {
                BigDecimal newBal = new BigDecimal(in.readLine());
                int newQuantity = Integer.parseInt(in.readLine());
                String s2 = String.format("%s $%,.2f", "your balance:", newBal);
                balText.setText(s2);
                String s3 = String.format("%s %s %s $%,.2f%s", "logged in as", username, "(balance:", newBal, ")");
                homeText.setText(s3);
                bal.bd = newBal;
                quantityOwned = newQuantity;
                qText.setText("shares owned: " + newQuantity + "\n");
                // update table with new quantity
                if (newQuantity == 0) table.getItems().remove(row);
                else {
                    String type = table.getItems().get(row).getType();
                    BigDecimal price = table.getItems().get(row).getPrice();
                    table.getItems().set(row, new Asset(stock, type, price, newQuantity));
                }
            } catch (IOException i) {
                System.out.println("IOException while getting server reply");
            }
        }
    }
}
