package app.web.pavelk.cloud1.client.controller;

import app.web.pavelk.cloud1.client.netty.Network;
import app.web.pavelk.cloud1.common.call.ClientMainContentsCallback;
import app.web.pavelk.cloud1.common.send.ProtoFileSender;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class MainController implements Initializable {

    public Button ClientConnectButton;
    public Text ClientConnectText;
    public VBox rootNode;
    public ListView filesListClientSize;
    public ListView filesLitServerSize;
    public HBox HBoxClientList;
    public TextField tfFileNameClient;
    public TextField tfFileNameServer;
    public ListView<String> filesListClient;
    public ListView<String> filesListServer;
    public VBox vBoxClient;

    private SimpleObjectProperty<ClientMainContentsCallback> contentsCallback;
    static final Logger rootLogger = LogManager.getRootLogger();
    private CountDownLatch networkStarter;
    private Thread t;
    private AuthorizationController authorizationController;
    private Stage newWindow;

    public MainController() {
        contentsCallback = new SimpleObjectProperty<>(this, "contentsCallback", new ClientMainContentsCallback() {

            @Override
            public void autOkClient() {
                updateUI(() -> {
                    if (newWindow != null) {
                        newWindow.close();
                    }
                    ClientConnectText.setText("подключен");
                    ClientConnectButton.setText("отключить");
                });
            }

            @Override
            public void autNotOkClient() {
                updateUI(() -> {
                    authorizationController.autText.setText("Неверный логин или пароль");
                });
            }

            @Override
            public void updateContentsCallBackClient(String name, long len) {
                updateUI(() -> {
                    filesListServer.getItems().add(name);
                    filesLitServerSize.getItems().add(len + " байт");
                });
            }

            @Override
            public void clearContentsCallBackClient() {
                updateUI(() -> {
                    if (newWindow != null) {
                        newWindow.close();
                    }
                    filesListServer.getItems().clear();
                    filesLitServerSize.getItems().clear();
                });
            }

            @Override
            public void clientUpdateContents() {
                refreshLocalFilesList();
            }

        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootLogger.info("Client initialize");
        refreshLocalFilesList();
    }

    public void ClientConnectButton(ActionEvent actionEvent) {
        if (ClientConnectButton.getText().equals("отключить")) {
            rootLogger.info("Client disconnect");
            Network.getInstance().stop();
            ClientConnectText.setText("отключен");
            ClientConnectButton.setText("подключить");
        } else {
            try {
                networkStarter = new CountDownLatch(1);
                t = new Thread(() -> Network.getInstance().start(contentsCallback.get(), networkStarter));
                t.start();
                networkStarter.await();
                rootLogger.info("Client connect");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                FXMLLoader fxmlLoaderR = new FXMLLoader(getClass().getResource("/fxml/authorization.fxml"));
                newWindow = new Stage();
                Stage stage = (Stage) ClientConnectButton.getScene().getWindow();
                Parent root = fxmlLoaderR.load();
                authorizationController = fxmlLoaderR.getController();
                Scene secondScene = new Scene(root);
                newWindow.setTitle("login");
                newWindow.setScene(secondScene);
                newWindow.initModality(Modality.WINDOW_MODAL);
                newWindow.initOwner(stage);
                newWindow.setX(stage.getX() + 200);
                newWindow.setY(stage.getY() + 100);
                newWindow.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void downloadWithServerFile() {// download
        if (checkingTheConnection()) return;

        String nameFile = filesListServer.getSelectionModel().getSelectedItem();
        if (nameFile == null) {
            if (tfFileNameClient.getLength() > 0) {
                nameFile = tfFileNameClient.getText();
                tfFileNameClient.clear();
            }
        }
        rootLogger.info("Client pressOnDownloadServer request Server send file - " + nameFile);
        ProtoFileSender.sendCommandByteAndOneString((byte) 30, nameFile, Network.getInstance().getCurrentChannel());
    }

    public void pressOnSendServer() { //send server

        if (checkingTheConnection()) return;

        String nameFile = filesListServer.getSelectionModel().getSelectedItem();
        if (nameFile == null) {
            if (tfFileNameServer.getLength() > 0) {
                nameFile = tfFileNameServer.getText();
                tfFileNameServer.clear();
            }
        }
        rootLogger.info("Client request Server send file - " + nameFile);
        ProtoFileSender.sendCommandByteAndOneString((byte) 30, nameFile, Network.getInstance().getCurrentChannel());

    }

    public void pressOnSendClient() { // send client

        if (checkingTheConnection()) return;
        try {
            String str = filesListClient.getSelectionModel().getSelectedItem();
            if (str == null) {
                if (tfFileNameClient.getLength() > 0) {
                    str = tfFileNameClient.getText();
                }
            }
            rootLogger.info("Client send file - " + str);

            String finalStr = str;
            ProtoFileSender.sendFile((byte) 25, Paths.get("Client/client_storage/" + str), Network.getInstance().getCurrentChannel(), future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                }
                if (future.isSuccess()) {
                    rootLogger.info("Client file sent - " + finalStr);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void deleteFile() { // delete client
        String str = filesListClient.getSelectionModel().getSelectedItem();
        if (str == null) {
            if (tfFileNameClient.getLength() > 0) {
                str = tfFileNameClient.getText();
            }
        }
        Paths.get("Client/client_storage/" + str).toFile().delete();
        refreshLocalFilesList();
        rootLogger.info("Client delete local file - " + str);
    }

    public void serverDelete() { // delete server

        if (checkingTheConnection()) return;

        String nameF = filesListServer.getSelectionModel().getSelectedItem();
        if (nameF == null) {
            if (tfFileNameServer.getLength() > 0) {
                nameF = tfFileNameClient.getText();
                if (nameF == "") return;
            }
        }
        rootLogger.info("Client request Server delete file - " + nameF);
        ProtoFileSender.sendCommandByteAndOneString((byte) 29, nameF, Network.getInstance().getCurrentChannel());
    }

    public void refreshLocalFilesList() { //update catalog client
        rootLogger.info("Client update local contents");
        updateUI(() -> {
            try {
                filesListClient.getItems().clear();
                filesListClientSize.getItems().clear();
                if (Files.exists(Paths.get("Client/client_storage"))) {
                    Files.list(Paths.get("Client/client_storage"))
                            .map(p -> new String[]{p.getFileName().toString(), p.toFile().length() + " байт"})
                            .forEach(o -> {
                                filesListClient.getItems().add(o[0]);
                                filesListClientSize.getItems().add(o[1]);
                            });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void serverRequestContents() { //update catalog server
        if (checkingTheConnection()) return;
        ProtoFileSender.sendCommandByte((byte) 18, Network.getInstance().getCurrentChannel());
        rootLogger.info("Client request Server contents");
    }

    public boolean checkingTheConnection() {
        if (Network.getInstance().getCurrentChannel() == null || !Network.getInstance().getCurrentChannel().isOpen()){
            new Thread(() -> {
                try {
                    ClientConnectText.styleProperty().bind(Bindings.concat(
                            "-fx-fill: rgb(255, 0, 0); -fx-font-size: 25; "));
                    Thread.sleep(100);
                    ClientConnectText.styleProperty().bind(Bindings.concat(
                            "-fx-fill: rgb(220, 200, 0); -fx-font-size: 20; "));
                    Thread.sleep(100);
                    ClientConnectText.styleProperty().bind(Bindings.concat(
                            "-fx-fill: rgb(255, 0, 0); -fx-font-size: 25; "));
                    Thread.sleep(100);
                    ClientConnectText.styleProperty().bind(Bindings.concat(
                            "-fx-fill: black; -fx-font-size: 15; "));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            return true;
        }
       return false;
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void exitAction() {
        rootLogger.info("Client exit");
        Network.getInstance().stop();
        Platform.exit();
    }
}
