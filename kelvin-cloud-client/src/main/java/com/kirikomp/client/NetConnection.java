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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


public class NetConnection {

    private Socket sock;
    private ObjectEncoderOutputStream out;
    private ObjectDecoderInputStream in;
    private SocketAddress addr;


    public NetConnection() {
        sock = new Socket();
        addr = new InetSocketAddress(ConfigSingleton.getInstance().HOST, ConfigSingleton.getInstance().PORT);
    }

    //Открываем соединение
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

    //Закрываем соединение
    public void close()
            throws IOException {
        if (sock.isClosed()) return;

        out.close();
        in.close();
        sock.close();
    }

    //Аутентификация
    public void auth(String login, String psw)
            throws SendDataException {
        if (login.trim().isEmpty() || psw.trim().isEmpty())
            return;

        DataPackage com = new AuthCommand(login.trim(), passToHash(psw.trim())); //удаляем пробелы с начала и конца строки переводим пароль в хеш и передаем
        sendToServer(com);
    }

    //Переводим пароль в хеш
    //todo Подумать на какой стороне лучше оставить
    private String passToHash(String password) {
        StringBuffer code = new StringBuffer(); //the hash code
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte bytes[] = password.getBytes();
            byte digest[] = messageDigest.digest(bytes); //create code
            for (int i = 0; i < digest.length; ++i) {
                code.append(Integer.toHexString(0x0100 + (digest[i] & 0x00FF)).substring(1));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Exception NoSuchAlgorithmException!");
        }
        return code.toString();
    }

    //Отправляем команду на отправку списка файлов
    public void sendFileListCommand()
            throws SendDataException {
        DataPackage com = new FileListCommand();
        sendToServer(com);
    }

    //Отправляем команду на скачивание файлов
    public void sendDownloadFilesCommand(List<String> filenames)
            throws SendDataException {
        if (filenames.isEmpty()) return;

        List<String> list = new ArrayList<>(filenames);
        DataPackage com = new GetFilesCommand(list);
        sendToServer(com);
    }

    //Отправляем команду на удаление списка файлов
    public void sendDeleteFilesCommand(List<String> filenames)
            throws SendDataException {
        if (filenames.isEmpty()) return;

        List<String> list = new ArrayList<>(filenames);
        DataPackage com = new DeleteFilesCommand(list);
        sendToServer(com);
    }

    //Получение ответа от сервера
    DataPackage getResponseFromServer()
            throws ServerResponseException {
        try {
            Object obj = in.readObject();
            return (DataPackage) obj;
        } catch (ClassNotFoundException | IOException e) {
            throw new ServerResponseException(e);
        }
    }

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

    //Приватный статический класс обработки исключений отправки
    public static class SendDataException
            extends Exception {

        private SendDataException(Throwable cause) {
            super(cause);
        }

    }

    //Приватный статический класс обработки исключений ответа сервера
    public static class ServerResponseException
            extends Exception {

        private ServerResponseException(Throwable cause) {
            super(cause);
        }


    }

}