package com.kirikomp.client;


import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import static java.lang.System.exit;
import static javafx.fxml.FXMLLoader.load;

public class Client
        extends Application {

    public Stage mainStage;

    private static NetConnection conn;

    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private static final String TITLE = "Kelvin Cloud Client";
    private static final String MAIN_FXML_PATH = "/main.fxml";
    private static final String NAME_CONF = "config.properties";

    // статический метод отдает текущее подключение
    public static NetConnection getNetConnection() {
        return conn;
    }

    /**
     * Старт приложения
     * @param primaryStage
     * @throws Exception
     */
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
            mainStage.setTitle(TITLE);
            mainStage.setOnHidden(e -> exit(0)); //Действие при закрытии приложения
            mainStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Остановка приложения
     * @throws Exception
     */
    @Override
    public void stop()
            throws Exception {
        conn.close();
        super.stop();
    }

    /**
     * Считываем данные из конфигурационного файла в singleton, и запуск приложения
     * @param args не применяются
     */
    public static void main(String... args) {
        ConfigSingleton props = ConfigSingleton.getInstance();
        Properties property = new Properties();

        // this is the path within the jar file
        InputStream input = Client.class.getClassLoader().getResourceAsStream(NAME_CONF);
        if (input == null) {
            // this is how we load file within editor (IDEA)
            input =  Client.class.getResourceAsStream("/resource" + NAME_CONF);
        }

        try {
            property.load(input);
            props.HOST = property.getProperty("host", "localhost");
            props.PORT = Integer.parseInt(property.getProperty("port", "1234"));
            props.STORAGE_DIR = property.getProperty("storage.dir", "client_storage");
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
}