package com.kirikomp.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.Properties;

public class ServerRunner {

    private static final String NAME_CONF = "config.properties";
    private static final String CTX = "spring-context.xml";

    protected static int PORT;
    protected static String STORAGE_DIR;
    protected static int MAX_OBJ_SIZE;

    /**
     * Считываем данные из конфигурационного файла в статические переменные и запускаем приложение
     * @param args порт на котором нужно стартовать сервер (опционально, по умолчанию берет из конфиг файла)
     */
    public static void main(String[] args) throws Exception {
        Properties property = new Properties();

        try  {
            property.load(Server.class.getClassLoader().getResourceAsStream(NAME_CONF));
            // если порт не указан в качестве входного параметра в аргументах, берем из файла конфигурации
            PORT = args.length > 0 ? Integer.parseInt(args[0]): Integer.parseInt(property.getProperty("port", "1234"));
            STORAGE_DIR = property.getProperty("storage.dir", "server_storage");
            MAX_OBJ_SIZE = Integer.parseInt(property.getProperty("max.obj.size", "52428800"));

            System.out.println("PORT: " + PORT
                    + ", STORAGE_DIR: " + STORAGE_DIR
                    + ", MAX_OBJ_SIZE: " + MAX_OBJ_SIZE);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения файла 'config.properties'!!! " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Неверный формат настроек сервера 'config.properties'!!! " + e.getMessage());
        }

        ApplicationContext context = new ClassPathXmlApplicationContext(CTX);
        Server server = context.getBean(Server.class);
        server.run();
    }
}
