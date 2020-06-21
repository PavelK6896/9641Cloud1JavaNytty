package app.web.pavelk.cloud1.client.controller;


import app.web.pavelk.cloud1.client.netty.Network;
import app.web.pavelk.cloud1.common.send.ProtoFileSender;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;


import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class AuthorizationController implements Initializable {

    public TextField loginField;
    public PasswordField passwordField;
    public VBox VBoxPenal;
    public CheckBox autChekBox;
    public Text autText;
    private String FILE_NAME = "Client/loginClient/login.txt";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (Files.exists(Paths.get(FILE_NAME))) {
            try {
                Files.lines(Paths.get(FILE_NAME), StandardCharsets.UTF_8)
                        .forEach(y -> {
                            String[] stl = y.split(" ");
                            loginField.setText(stl[0]);
                            passwordField.setText(stl[1]);
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void ConnectButton() {

        if (loginField.getText()
                .equals("")) {
            loginField.setPromptText("Ведите логин");
            return;
        }
        if (passwordField.getText()
                .equals("")) {
            passwordField.setPromptText("Ведите пароль");
            return;
        }

        if (autChekBox.isSelected()) {
            if (!Files.exists(Paths.get("Client/loginClient"))) {
                try {
                    Files.createDirectories(Paths.get("Client/loginClient"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                ArrayList<String> con = new ArrayList<>();
                con.add(loginField.getText() + " " + passwordField.getText());
                Files.write(Paths.get(FILE_NAME), con, StandardCharsets.UTF_8);


            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (Files.exists(Paths.get(FILE_NAME))) {
                Paths.get(FILE_NAME).toFile().delete();
            }
        }

        ProtoFileSender.sendCommandByteAndTwoString((byte) 26, loginField.getText(), passwordField.getText(), Network.getInstance().getCurrentChannel());
    }

}
