package com.kirikomp.client;


import com.kirikomp.common.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;


public class NetConnection {

    private Socket sock;
    private ObjectEncoderOutputStream out;
    private ObjectDecoderInputStream in;
    private SocketAddress addr;

    /**
     * Конструктор подключения
     */
    public NetConnection() {
        sock = new Socket();
        addr = new InetSocketAddress(ConfigSingleton.getInstance().HOST, ConfigSingleton.getInstance().PORT);
    }

    /**
     * Открытие соединения
     */
    public void open()
            throws IOException {
        if (!sock.isClosed() && sock.isConnected()) return;

        sock = new Socket();
        sock.connect(addr);

        OutputStream os = sock.getOutputStream();
        InputStream is = sock.getInputStream();
        out = new ObjectEncoderOutputStream(os);
        in = new ObjectDecoderInputStream(is, ConfigSingleton.getInstance().MAX_OBJ_SIZE);
    }

    /**
     * Закрытие соединения
     */
    public void close()
            throws IOException {
        if (sock.isClosed()) return;

        out.close();
        in.close();
        sock.close();
    }

    /**
     * Аутентификация
     * @param login String Login
     * @param psw String Password
     */
    public void auth(String login, String psw)
            throws SendDataException {
        if (login.trim().isEmpty() || psw.trim().isEmpty())
            return;

        DataPackage com = new AuthCommand(login.trim(), psw.trim()); //удаляем пробелы с начала и конца строки и отправляем
        sendToServer(com);
    }

    /**
     *  Метод для отправки списка файлов
     */
    public void sendFileListCommand()
            throws SendDataException {
        DataPackage com = new FileListCommand();
        sendToServer(com);
    }

    /**
     *  Метод на скачивание файлов с сервера
     * @param filenames List<String> list name files
     */
    public void sendDownloadFilesCommand(List<String> filenames)
            throws SendDataException {
        if (filenames.isEmpty()) return;

        List<String> list = new ArrayList<>(filenames);
        DataPackage com = new GetFilesCommand(list);
        sendToServer(com);
    }

    /**
     *  Метод на удаление списка файлов с сервера
     *  @param filenames List<String> list name files
     */
    public void sendDeleteFilesCommand(List<String> filenames)
            throws SendDataException {
        if (filenames.isEmpty()) return;

        List<String> list = new ArrayList<>(filenames);
        DataPackage com = new DeleteFilesCommand(list);
        sendToServer(com);
    }

    /**
     *  Метод на получение ответа от сервера
     * @return DataPackage response server
     */
    DataPackage getResponseFromServer()
            throws ServerResponseException {
        try {
            Object obj = in.readObject();
            return (DataPackage) obj;
        } catch (ClassNotFoundException | IOException e) {
            throw new ServerResponseException(e);
        }
    }

    /**
     *  Метод на отпавку файлов на сервер
     *  @param data DataPackage
     */
    //Отправляем файл на сервер
    public void sendToServer(DataPackage data)
            throws SendDataException {
        try {
            out.writeObject(data);
            out.flush();
        } catch (IOException e) {
            throw new SendDataException(e);
        }
    }

    /**
     *  Вложенный приватный статический класс обработки исключений при отправке
     */
    public static class SendDataException
            extends Exception {

        private SendDataException(Throwable cause) {
            super(cause);
        }

    }

    /**
     *  Вложенный приватный статический класс обработки исключений при получении
     */
    public static class ServerResponseException
            extends Exception {

        private ServerResponseException(Throwable cause) {
            super(cause);
        }


    }

}