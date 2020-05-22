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



    public NetConnection() {
        sock = new Socket();
        addr = new InetSocketAddress(ConfigSingleton.getInstance().HOST, ConfigSingleton.getInstance().PORT);
    }


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


    public void close()
            throws IOException {
        if (sock.isClosed()) return;

        out.close();
        in.close();
        sock.close();
    }


    public void auth(String login, String psw)
            throws SendDataException {
        if (login.trim().isEmpty() || psw.trim().isEmpty())
            return;

        DataPackage com = new AuthCommand(login.trim(), psw.trim());
        sendToServer(com);
    }


    public void sendFileListCommand()
            throws SendDataException {
        DataPackage com = new FileListCommand();
        sendToServer(com);
    }


    public void sendDownloadFilesCommand(List<String> filenames)
            throws SendDataException {
        if (filenames.isEmpty()) return;

        List<String> list = new ArrayList<>(filenames);
        DataPackage com = new GetFilesCommand(list);
        sendToServer(com);
    }


    public void sendDeleteFilesCommand(List<String> filenames)
            throws SendDataException {
        if (filenames.isEmpty()) return;

        List<String> list = new ArrayList<>(filenames);
        DataPackage com = new DeleteFilesCommand(list);
        sendToServer(com);
    }


    DataPackage getResponseFromServer()
            throws ServerResponseException {
        try {
            Object obj = in.readObject();
            return (DataPackage) obj;
        } catch (ClassNotFoundException | IOException e) {
            throw new ServerResponseException(e);
        }
    }


    public void sendToServer(DataPackage data)
            throws SendDataException {
        try {
            out.writeObject(data);
            out.flush();
        } catch (IOException e) {
            throw new SendDataException(e);
        }
    }


    public static class SendDataException
            extends Exception {

        private SendDataException(Throwable cause) {
            super(cause);
        }

    }


    public static class ServerResponseException
            extends Exception {

        private ServerResponseException(Throwable cause) {
            super(cause);
        }


    }

}