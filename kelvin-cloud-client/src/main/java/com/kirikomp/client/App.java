package com.kirikomp.client;


import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static javafx.fxml.FXMLLoader.load;

public class App
        extends Application {

    public Stage mainStage;

    private static NetConnection conn;

    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private static final String TITLE = "Kelvin Cloud Client";
    private static final String MAIN_FXML_PATH = "/main.fxml";

    // статически инициализируем канал  связи с сервером
    public static NetConnection getNetConnection() {
        return conn;
    }

    @Override
    public void start(Stage primaryStage)
            throws Exception {

        mainStage = primaryStage;
        conn.open();

        URL res = getClass().getResource(MAIN_FXML_PATH);

        try {
            Parent root = load(res);
            Scene scene = new Scene(root, WIDTH, HEIGHT);
            mainStage.setScene(scene);
            mainStage.setTitle(TITLE + " - Авторизация");
//            Controller controller = loader.getController();
//            mainStage.setOnHidden(e -> controller.shutdown());
            mainStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void stop()
            throws Exception {
        conn.close();

        super.stop();
    }

    public static void main(String... args) {
        ConfigSingleton props = ConfigSingleton.getInstance();
        Properties property = new Properties();
        Path path = Paths.get("kelvin-cloud-client", "src", "main", "resources", "config.properties");

        try (FileInputStream fis = new FileInputStream(path.toString())) {
            property.load(fis);
            props.HOST = property.getProperty("host", "localhost");
            props.PORT = Integer.parseInt(property.getProperty("port", "1234"));
            props.STORAGE_DIR = property.getProperty("storage.dir", "client/server_storage");
            props.MAX_OBJ_SIZE = Integer.parseInt(property.getProperty("max.obj.size", "52428800"));

            System.out.println("HOST: " + ConfigSingleton.getInstance().HOST
                    + ", PORT: " + ConfigSingleton.getInstance().PORT
                    + ", STORAGE_DIR: " + ConfigSingleton.getInstance().STORAGE_DIR
                    + ", MAX_OBJ_SIZE: " + ConfigSingleton.getInstance().MAX_OBJ_SIZE);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения файла 'config.properties'!!! " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Неверный формат настроек сервера 'config.properties'!!! " + e.getMessage());
        }
        conn = new NetConnection();
        launch(args);
    }

    public void setStageTitle(String newTitle) {
        mainStage.setTitle(TITLE + newTitle);
    }

}