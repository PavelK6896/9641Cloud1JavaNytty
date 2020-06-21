package app.web.pavelk.cloud1.client;

import app.web.pavelk.cloud1.client.netty.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;


public class ProtoClient extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("Cloud");
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(o -> {
            Network.getInstance().stop();
            LogManager.getRootLogger().info("Stage close");
        });
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }
}
