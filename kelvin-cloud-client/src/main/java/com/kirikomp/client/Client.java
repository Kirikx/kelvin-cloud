package com.kirikomp.client;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import static java.lang.System.exit;

public class Client
        extends Application {

    private static final String TITLE = "Kelvin Cloud Client";
    private static final String MAIN_FXML_PATH = "/main.fxml";
    private static final String NAME_CONF = "config.properties";

    private static NetConnection conn;

    private final int WIDTH = 800;
    private final int HEIGHT = 800;

    public static ApplicationContext context;

    // статический метод отдает текущее подключение
    public static NetConnection getNetConnection() {
        return conn;
    }

    private static Object call(Class<?> cls) {
        return context.getBean(cls);
    }

    /**
     * Старт приложения
     *
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage)
            throws Exception {

        conn.open();

        URL res = getClass().getResource(MAIN_FXML_PATH);

        try {
            FXMLLoader loader = new FXMLLoader(res);
            loader.setControllerFactory(clr -> context.getBean(clr));
            Scene scene = new Scene(loader.load(), WIDTH, HEIGHT);
            primaryStage.setScene(scene);
            primaryStage.setTitle(TITLE);
            primaryStage.setOnHidden(e -> exit(0)); //Действие при закрытии приложения
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Остановка приложения
     *
     * @throws Exception
     */
    @Override
    public void stop()
            throws Exception {
        conn.close();
        super.stop();
    }

    /**
     * Считываем данные из конфигурационного файла в singleton, и запускаем приложения
     *
     * @param args не применяются
     */
    public static void main(String... args) {
        ConfigSingleton props = ConfigSingleton.getInstance();
        Properties property = new Properties();

        // this is the path within the jar file
        InputStream input = Client.class.getClassLoader().getResourceAsStream(NAME_CONF);
        if (input == null) {
            // this is how we load file within editor (IDEA)
            input = Client.class.getResourceAsStream("/resource" + NAME_CONF);
        }

        try {
            property.load(input);
            props.setHost(property.getProperty("host", "localhost"));
            props.setPort(Integer.parseInt(property.getProperty("port", "1234")));
            props.setStorageDir(property.getProperty("storage.dir", "client_storage"));
            props.setMaxObjSize(Integer.parseInt(property.getProperty("max.obj.size", "52428800")));

            System.out.println("HOST: " + ConfigSingleton.getInstance().getHost()
                    + ", PORT: " + ConfigSingleton.getInstance().getPort()
                    + ", STORAGE_DIR: " + ConfigSingleton.getInstance().getStorageDir()
                    + ", MAX_OBJ_SIZE: " + ConfigSingleton.getInstance().getMaxObjSize());

            new File(ConfigSingleton.getInstance().getStorageDir()).mkdirs();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения файла 'config.properties'!!! " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Неверный формат настроек сервера 'config.properties'!!! " + e.getMessage());
        }

        context = new AnnotationConfigApplicationContext(SpringConfig.class);
        conn = context.getBean(NetConnection.class);

        launch(args);
    }

}