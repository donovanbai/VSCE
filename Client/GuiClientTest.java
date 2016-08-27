import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
 
public class GuiClientTest extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
		InetAddress hostIP = InetAddress.getByName("162.156.144.68");
        int portNumber = 10000;
 
		Socket echoSocket = new Socket(hostIP, portNumber);
		PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
		BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        primaryStage.setTitle("JavaFX Welcome");
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25)); //top, right, bottom, left

		Text scenetitle = new Text("Welcome");
		scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
		grid.add(scenetitle, 0, 0, 2, 1); //column, row, column span, row span
		
		Label userName = new Label("Username:");
		grid.add(userName, 0, 1);
	
		TextField userField = new TextField();
		grid.add(userField, 1, 1);
		
		Label pw = new Label("Password:");
		grid.add(pw, 0, 2);
		
		PasswordField pwField = new PasswordField();
		grid.add(pwField, 1, 2);

		Button signInBtn = new Button("Sign in");
		Button registerBtn = new Button("Register");

		HBox hbButton = new HBox(10);
		hbButton.setAlignment(Pos.BOTTOM_RIGHT);
		hbButton.getChildren().addAll(signInBtn, registerBtn);
		grid.add(hbButton, 0, 4, 2, 1);

		final Text actiontarget = new Text();
		grid.add(actiontarget, 1, 6);

		signInBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				out.println("sign in");
				out.println(userField.getText());
				out.println(pwField.getText());
				String serverReply = null;
				try {
					serverReply = in.readLine();
				} catch (IOException i) {
					System.out.println("IOException while getting server reply");
				}
				actiontarget.setFill(Color.GREEN);
				actiontarget.setText(serverReply);
			}
		});

		registerBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
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
					actiontarget.setFill(Color.GREEN);
					actiontarget.setText("registration successful!");
				}
				else if (serverReply.equals("1")) {
					actiontarget.setFill(Color.RED);	
					actiontarget.setText("username already taken");
				}
			}
		});

		//grid.setGridLinesVisible(true);

		Scene scene = new Scene(grid, 300, 275);
		primaryStage.setScene(scene);        
        primaryStage.show();
    }

	public static void main(String[] args) {
        launch(args);
    }
}
