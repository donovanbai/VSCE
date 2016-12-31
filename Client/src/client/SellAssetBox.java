package client;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Locale;

public class SellAssetBox {
    PrintWriter out;
    BufferedReader in;
    String name, username;
    TextField qField = new TextField();
    Text msg = new Text();
    Text balText, homeText, qText;
    int row;
    BigDecimal quantityOwned;
    BigDecimalWrapper bal;
    TableView<Asset> table;
    
    public void display(String type, String name, BigDecimal price, BigDecimalWrapper bal, BigDecimal quantityOwned, Text homeText, String username, TableView<Asset> table, int row, PrintWriter out, BufferedReader in) {
        this.name = name;
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
        window.setTitle("Sell " + name);
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(15);
        grid.setVgap(15);
        
        String s = String.format("%s $%,.2f", "your balance:", bal.bd);
        balText = new Text(s);
        balText.setFont(Font.font("Calibri", 20));
        grid.add(balText, 0, 0);
        GridPane.setHalignment(balText, HPos.CENTER);
        
        String s2 = null;
        if (type.equals("stock")) s2 = "shares owned: " + quantityOwned + "\n";
        else if (type.equals("bitcoin")) s2 = "amount owned: " + quantityOwned + " XBT\n";
        else if (type.equals("ether")) s2 = "amount owned: " + quantityOwned + " ETH\n";
        qText = new Text(s2);
        qText.setFont(Font.font("Calibri", 20));
        grid.add(qText, 0, 1);
        GridPane.setHalignment(qText, HPos.CENTER);
        
        Label quantity = new Label("quantity");
        quantity.setFont(Font.font("Calibri", 20));
        
        qField.setFont(Font.font("Calibri", 20));
        qField.setOnAction(e -> {
            if (type.equals("stock")) sellStock();
            else if (type.equals("bitcoin") || type.equals("ether")) sellCrypto();
        });
        
        HBox hbox = new HBox(15);
        hbox.getChildren().addAll(quantity, qField);
        grid.add(hbox, 0, 2);
        GridPane.setHalignment(hbox, HPos.CENTER);

        Text totalText = new Text();
        
        StringConverter<Number> converter = new NumberStringConverter();
        Locale locale = Locale.CANADA;
        if (type.equals("stock")) {
            IntegerProperty ip = new SimpleIntegerProperty();
            Bindings.bindBidirectional(qField.textProperty(), ip, converter);         
            totalText.textProperty().bind(Bindings.format(locale, "estimated total: $%,.2f", ip.multiply(price.doubleValue())));
        }
        else if (type.equals("bitcoin") || type.equals("ether")) {
            DoubleProperty dp = new SimpleDoubleProperty();
            Bindings.bindBidirectional(qField.textProperty(), dp, converter);         
            totalText.textProperty().bind(Bindings.format(locale, "estimated total: $%,.2f", dp.multiply(price.doubleValue())));
        }
        totalText.setFont(Font.font("Calibri", 20));
        totalText.setFill(Color.GREEN);
        grid.add(totalText, 0, 3);
        GridPane.setHalignment(totalText, HPos.CENTER);
        
        Button sellBtn = new Button("Sell now");
        sellBtn.setFont(Font.font("Calibri", 20));
        sellBtn.setOnAction(e -> {
            if (type.equals("stock")) sellStock();
            else if (type.equals("bitcoin") || type.equals("ether")) sellCrypto();
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
        if (Integer.parseInt(qField.getText()) > quantityOwned.intValue()) {
            msg.setFill(Color.RED);
            msg.setText("you do not own that many shares");
            return;
        }
        
        ObjectProperty textColorProperty = new SimpleObjectProperty();
        Task task = new Task<String>() {
            @Override
            protected String call() {
                textColorProperty.setValue(Color.GREEN);
                updateMessage("loading...");
                out.println("sell stock");
                out.println(name);
                out.println(qField.getText());
                String serverReply = null;
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {
                    System.out.println("IOException while getting server reply");
                }
                return serverReply;    
            }
        };
        msg.fillProperty().bind(textColorProperty);
        msg.textProperty().bind(task.messageProperty());
        new Thread(task).start();
        task.setOnSucceeded(e->{
            msg.fillProperty().unbind();
            msg.textProperty().unbind();
            String serverReply = (String)task.getValue();
            if (serverReply == null) {
                msg.setFill(Color.RED);
                msg.setText("IOException while getting server reply");
            }
            else if (serverReply.equals("fail") || serverReply.equals("N/A")) {
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
                    BigDecimal newQuantity = new BigDecimal(in.readLine());
                    String s = String.format("%s $%,.2f", "your balance:", newBal);
                    balText.setText(s);
                    String s2 = String.format("%s %s %s $%,.2f%s", "logged in as", username, "(balance:", newBal, ")");
                    homeText.setText(s2);
                    bal.bd = newBal;
                    quantityOwned = newQuantity;
                    qText.setText("shares owned: " + newQuantity + "\n");
                    // update table with new quantity and new gain/loss
                    Asset.totalTotalVal = Asset.totalTotalVal.subtract(table.getItems().get(row).getTotalVal());
                    Asset.totalGain = Asset.totalGain.subtract(table.getItems().get(row).getGain());
                    if (newQuantity.compareTo(new BigDecimal("0")) == 0) table.getItems().remove(row);
                    else {
                        String type = table.getItems().get(row).getType();
                        BigDecimal price = table.getItems().get(row).getPrice();
                        BigDecimal newOrig = new BigDecimal(in.readLine());
                        table.getItems().set(row, new Asset(name, type, price, newQuantity, newOrig));
                    }
                    ObservableList<Asset> items = table.getItems();
                    items.set(items.size()-1, new Asset("total", Asset.totalTotalVal, Asset.totalGain));
                } catch (IOException i) {
                    System.out.println("IOException while getting server reply");
                }
            }
        });
    }
    
    private void sellCrypto() {
        BigDecimal quantity;
        try {
            quantity = new BigDecimal(qField.getText());
        } catch (NumberFormatException e) {
            msg.setFill(Color.RED);
            msg.setText("invalid input");
            return;
        }
        if (quantity.compareTo(new BigDecimal("0")) != 1) {
            msg.setFill(Color.RED);
            msg.setText("quantity has to be > 0");
            return;
        }
        if (quantity.compareTo(quantityOwned) == 1) {
            msg.setFill(Color.RED);
            if (name.equals("btc")) msg.setText("you do not own that much bitcoin");
            else if (name.equals("eth")) msg.setText("you do not own that much ether");
            return;
        }
        // check that number of decimal places is at most 8
        if (quantity.scale() > 8) {
            msg.setFill(Color.RED);
            msg.setText("too many decimal places");
            return;
        }
        
        ObjectProperty textColorProperty = new SimpleObjectProperty();
        Task task = new Task<String>() {
            @Override
            protected String call() {
                textColorProperty.setValue(Color.GREEN);
                updateMessage("loading...");
                if (name.equals("btc")) out.println("sell btc");
                else if (name.equals("eth")) out.println("sell eth");
                out.println(qField.getText());
                String serverReply = null;
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {
                    System.out.println("IOException while getting server reply");
                }
                return serverReply;    
            }
        };
        msg.fillProperty().bind(textColorProperty);
        msg.textProperty().bind(task.messageProperty());
        new Thread(task).start();
        task.setOnSucceeded(e->{
            msg.fillProperty().unbind();
            msg.textProperty().unbind();
            String serverReply = (String)task.getValue();
            if (serverReply == null) {
                msg.setFill(Color.RED);
                msg.setText("IOException while getting server reply");
            }
            else if (serverReply.equals("1")) {
                msg.setFill(Color.RED);
                if (name.equals("btc")) msg.setText("you do not own that much bitcoin. re-login.");
                else if (name.equals("eth")) msg.setText("you do not own that much ether. re-login.");
            }
            else if (serverReply.equals("fail")) {
                msg.setFill(Color.RED);
                if (name.equals("btc")) msg.setText("failed to retrieve bitcoin price");
                else if (name.equals("eth")) msg.setText("failed to retrieve ether price");
            }
            else if (serverReply.equals("0")) {
                msg.setFill(Color.GREEN);
                msg.setText("sale complete!"); 
                try {
                    quantityOwned = new BigDecimal(in.readLine());
                    bal.bd = new BigDecimal(in.readLine());
                    String s = String.format("%s $%,.2f", "your balance:", bal.bd);
                    balText.setText(s);
                    String s2 = String.format("%s %s %s $%,.2f%s", "logged in as", username, "(balance:", bal.bd, ")");
                    homeText.setText(s2);
                    if (name.equals("btc")) qText.setText("amount owned: " + quantityOwned + " XBT\n");
                    else if (name.equals("eth")) qText.setText("amount owned: " + quantityOwned + " ETH\n");
                    // update table with new quantity and new gain/loss
                    Asset.totalTotalVal = Asset.totalTotalVal.subtract(table.getItems().get(row).getTotalVal());
                    Asset.totalGain = Asset.totalGain.subtract(table.getItems().get(row).getGain());
                    if (quantityOwned.compareTo(new BigDecimal("0")) == 0) table.getItems().remove(row);
                    else {
                        String type = table.getItems().get(row).getType();
                        BigDecimal price = table.getItems().get(row).getPrice();
                        BigDecimal newOrig = new BigDecimal(in.readLine());
                        table.getItems().set(row, new Asset(name, type, price, quantityOwned, newOrig));
                    }
                    ObservableList<Asset> items = table.getItems();
                    items.set(items.size()-1, new Asset("total", Asset.totalTotalVal, Asset.totalGain));
                } catch (IOException i) {
                    System.out.println("IOException while getting server reply");
                }
            }
        });
    }
}
