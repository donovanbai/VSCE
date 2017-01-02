package client;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.concurrent.Task;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

public class BuyAssetBox {
    PrintWriter out;
    BufferedReader in;
    String asset, username;
    TextField qField = new TextField();
    Text msg = new Text();
    Text balText, homeText;
    BigDecimalWrapper bal;

    public void display(boolean isStock, String asset, BigDecimal price, BigDecimalWrapper bal, Text homeText, String username, PrintWriter out, BufferedReader in) {
        this.out = out;
        this.in = in;
        this.asset = asset;
        this.homeText = homeText;
        this.username = username;
        this.bal = bal;
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Buy " + asset);
        
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
            if (isStock) buyStock();
            else buyCurrency();
        });
        
        HBox hbox = new HBox(15);
        hbox.getChildren().addAll(quantity, qField);
        grid.add(hbox, 0, 1);
        GridPane.setHalignment(hbox, HPos.CENTER);

        Text costText = new Text();

        StringConverter<Number> converter = new NumberStringConverter();
        Locale locale = Locale.CANADA;
        if (isStock) {
            IntegerProperty ip = new SimpleIntegerProperty();
            Bindings.bindBidirectional(qField.textProperty(), ip, converter);
            costText.textProperty().bind(Bindings.format(locale, "estimated cost: $%,.2f", ip.multiply(price.doubleValue())));
        }
        else {
            DoubleProperty dp = new SimpleDoubleProperty();
            Bindings.bindBidirectional(qField.textProperty(), dp, converter);
            costText.textProperty().bind(Bindings.format(locale, "estimated cost: $%,.2f", dp.multiply(price.doubleValue())));
        }

        costText.setFont(Font.font("Calibri", 20));
        costText.setFill(Color.GREEN);
        grid.add(costText, 0, 2);
        GridPane.setHalignment(costText, HPos.CENTER);
        
        Button buyBtn = new Button("Buy now");
        buyBtn.setFont(Font.font("Calibri", 20));
        buyBtn.setOnAction(e -> {
            if (isStock) buyStock();
            else buyCurrency();
        });
        grid.add(buyBtn, 0, 3);
        GridPane.setHalignment(buyBtn, HPos.CENTER);
        
        msg.setFont(Font.font("Calibri", 20));
        grid.add(msg, 0, 5);
        GridPane.setHalignment(msg, HPos.CENTER);
          
        Scene scene = new Scene(grid, 500, 300);
        window.setScene(scene);
        window.showAndWait();
    }
    
    private void buyStock() {
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
        
        ObjectProperty textColorProperty = new SimpleObjectProperty();
        Task task = new Task<String>() {
            @Override
            protected String call() {
                textColorProperty.setValue(Color.GREEN);
                updateMessage("loading...");
                out.println("buy stock");
                out.println(asset);
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
            else if (serverReply.equals("fail")) {
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
        });   
    }

    private void buyCurrency() {
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
        if (quantity.scale() > Currency.getInstance(asset.toUpperCase()).getDefaultFractionDigits()) {
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
                out.println("buy currency");
                out.println(asset);
                out.println(qField.getText());
                String serverReply = null;
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {} // handled later
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
            else if (serverReply.equals("fail")) {
                msg.setFill(Color.RED);
                msg.setText("exchange rate could not be retrieved");
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
                    msg.setFill(Color.RED);
                    msg.setText("IOException while getting server reply");
                }
            }
        });
    }
}