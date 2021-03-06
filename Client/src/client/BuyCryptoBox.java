package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Locale;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
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

public class BuyCryptoBox {
    PrintWriter out;
    BufferedReader in;
    String username;
    TextField amtField = new TextField();
    Text msg = new Text();
    Text balText, homeText, costText, amtText;
    BigDecimalWrapper bal;
    BigDecimal price;
    String name;

    public void display(String name, BigDecimalWrapper bal, Text homeText, String username, PrintWriter out, BufferedReader in) {
        this.name = name;
        this.out = out;
        this.in = in;
        this.homeText = homeText;
        this.username = username;
        this.bal = bal;

        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        if (name.equals("btc")) window.setTitle("Buy bitcoin");
        else if (name.equals("eth")) window.setTitle("Buy ether");
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(15);
        grid.setVgap(15);
        
        String s = String.format("%s $%,.2f", "your balance:", bal.bd);
        balText = new Text(s);
        balText.setFont(Font.font("Calibri", 20));
        grid.add(balText, 0, 0);
        GridPane.setHalignment(balText, HPos.CENTER);
        
        costText = new Text();
        costText.setFont(Font.font("Calibri", 20));
        fetchPrice();
        grid.add(costText, 0, 1);
        GridPane.setHalignment(costText, HPos.CENTER);
        
        Label amount = new Label("amount ($)");
        amount.setFont(Font.font("Calibri", 20));
        
        amtField.setFont(Font.font("Calibri", 20));
        amtField.setOnAction(e -> {
            buy();
        });
        
        HBox hbox = new HBox(15);
        hbox.getChildren().addAll(amount, amtField);
        grid.add(hbox, 0, 2);
        GridPane.setHalignment(hbox, HPos.CENTER);
    
        amtText = new Text();     
        amtText.setFont(Font.font("Calibri", 20));
        amtText.setFill(Color.GREEN);
        grid.add(amtText, 0, 3);
        GridPane.setHalignment(amtText, HPos.CENTER);
        
        Button buyBtn = new Button("Buy now");
        buyBtn.setFont(Font.font("Calibri", 20));
        buyBtn.setOnAction(e-> {
            buy();
        });
        grid.add(buyBtn, 0, 4);
        GridPane.setHalignment(buyBtn, HPos.CENTER);
        
        msg.setFont(Font.font("Calibri", 20));
        grid.add(msg, 0, 6);
        GridPane.setHalignment(msg, HPos.CENTER);
          
        Scene scene = new Scene(grid, 500, 300);
        window.setScene(scene);
        window.showAndWait();
    }
    
    private void fetchPrice() { // fetch bitcoin price to display on UI
        ObjectProperty textColorProperty = new SimpleObjectProperty();
        Task task = new Task<Integer>() {
            @Override
            protected Integer call() {
                textColorProperty.setValue(Color.GREEN);
                updateMessage("loading...\n");
                if (name.equals("btc")) out.println("get btc price");
                else if (name.equals("eth")) out.println("get eth price");
                String serverReply;
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("IOException while getting server reply\n");
                    return 1;
                }
                if (serverReply.equals("fail")) {
                    textColorProperty.setValue(Color.RED);
                    updateMessage("failed to retrive price\n");
                    return 1;
                }
                price = new BigDecimal(serverReply);
                return 0;
            }
        };
        costText.fillProperty().bind(textColorProperty);
        costText.textProperty().bind(task.messageProperty());
        new Thread(task).start();
        task.setOnSucceeded(e->{
            costText.fillProperty().unbind();
            costText.textProperty().unbind();
            int result = (int)task.getValue();
            if (result == 0) {
                if (name.equals("btc")) costText.setText("price of 1 bitcoin: $" + price + "\n");
                else if (name.equals("eth")) costText.setText("price of 1 ether: $" + price + "\n");
                DoubleProperty dp = new SimpleDoubleProperty();
                StringConverter<Number> converter = new NumberStringConverter();
                Bindings.bindBidirectional(amtField.textProperty(), dp, converter);    
                Locale locale = Locale.CANADA;
                if (name.equals("btc")) amtText.textProperty().bind(Bindings.format(locale, "estimated amount: %,.8f XBT", dp.divide(price.doubleValue())));
                else if (name.equals("eth")) amtText.textProperty().bind(Bindings.format(locale, "estimated amount: %,.8f ETH", dp.divide(price.doubleValue())));
            }
        });
    }
    
    private void buy() {
        BigDecimal amt;
        try {
            amt = new BigDecimal(amtField.getText());
        } catch (NumberFormatException e) {
            msg.setFill(Color.RED);
            msg.setText("invalid input");
            return;
        }
        if (amt.compareTo(new BigDecimal("0")) != 1) {
            msg.setFill(Color.RED);
            msg.setText("quantity has to be > 0");
            return;
        }
        if (amt.compareTo(bal.bd) == 1) {
            msg.setFill(Color.RED);
            msg.setText("insufficient balance!");
            return;
        }
        // check that number of decimal places is at most 2
        if (amt.scale() > 2) {
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
                if (name.equals("btc")) out.println("buy btc");
                else if (name.equals("eth")) out.println("buy eth");
                out.println(amtField.getText());
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
                if (name.equals("btc")) msg.setText("bitcoin price could not be retrieved");
                else if (name.equals("eth")) msg.setText("ether price could not be retrieved");
            }
            else if (serverReply.equals("1")) {
                msg.setFill(Color.RED);
                msg.setText("insufficient balance!");
            }
            else if (serverReply.equals("0")) {
                msg.setFill(Color.GREEN);
                msg.setText("purchase complete!"); 
                try {
                    BigDecimal newQuantity = new BigDecimal(in.readLine());
                    BigDecimal newBal = new BigDecimal(in.readLine());
                    String s = String.format("%s $%,.2f", "your balance:", newBal);
                    balText.setText(s);
                    String s2 = String.format("%s %s %s $%,.2f%s", "logged in as", username, "(balance:", newBal, ")");
                    homeText.setText(s2);
                    bal.bd = newBal;
                } catch (IOException i) {
                    System.out.println("IOException while getting server reply");
                }
            }
        });
    }
}
