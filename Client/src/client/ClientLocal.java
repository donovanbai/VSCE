package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javafx.application.Application;
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
    static Scene login, home;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        initVars();
        setupLogin();
        setupHome();

        window = primaryStage;
        window.setTitle("VSCE");
        window.setScene(login);        
        window.show();
    }

    public static void initVars() {
        try {
            hostIP = InetAddress.getLocalHost();
            socket = new Socket(hostIP, portNumber);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());          
        }
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
        
        Text msg = new Text();
        grid.add(msg, 1, 6);

        Button signInBtn = new Button("Sign in");
        signInBtn.setOnAction(e -> {
            String username = userField.getText();
            String pw = pwField.getText();
            
            if (username.equals("") || pw.equals("")) {
                msg.setFill(Color.RED);
                msg.setText("username and password cannot be empty");
            }
            else {
                out.println("sign in");
                out.println(username);
                out.println(pw);
                String serverReply = null;
                try {
                    serverReply = in.readLine();
                } catch (IOException i) {
                    System.out.println("IOException while getting server reply");
                }
                if (serverReply.equals("0")) {
                    window.setScene(home);
                    window.setFullScreen(true);
                }
                else if (serverReply.equals("1")) {
                    msg.setFill(Color.RED);
                    msg.setText("username does not exist");
                }
                else if (serverReply.equals("2")) {
                    msg.setFill(Color.RED);
                    msg.setText("incorrect password");
                }
            }
        });
        
        Button registerBtn = new Button("Register");
        registerBtn.setOnAction(e -> {
            String username = userField.getText();
            String pw = pwField.getText();
            
            if (username.equals("") || pw.equals("")) {
                msg.setFill(Color.RED);
                msg.setText("username and password cannot be empty");
            }
            else {
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
                    msg.setFill(Color.GREEN);
                    msg.setText("registration successful!");
                }
                else if (serverReply.equals("1")) {
                    msg.setFill(Color.RED);
                    msg.setText("username already taken");
                }
            }
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
        grid.setHgap(10);
        //grid.setPadding(new Insets(25, 25, 25, 25));
        
        Text sceneTitle = new Text("HOME SCREEN: NOTHING TO SEE HERE YET");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 40));
        grid.add(sceneTitle, 0, 0);
        
        Text temp = new Text("PS. MOOSE SAYS HE DOESN'T DESERVE TO GO OUT LOOOOOL");
        temp.setFont(Font.font("Tahoma", FontWeight.NORMAL, 40));
        grid.add(temp, 0, 1);
        
        Text msg = new Text();
        grid.add(msg, 0, 3);
        
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> window.setScene(login));
        grid.add(logoutBtn, 0, 2);
      
        home = new Scene(grid, 500, 500);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
