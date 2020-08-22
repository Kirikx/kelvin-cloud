package com.kirikomp.client;


import com.kirikomp.common.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Thread.currentThread;

@Component
public class ServerResponseHandler
        implements Runnable {
    @Autowired
    private NetConnection netConnection;
    private Consumer<List<String>> callbackFileList;
    private Runnable callbackFileData;
    private FileChunkSaver saver;

    /**
     * Конструктор хендлера сервера
     */
    public ServerResponseHandler() {
        saver = new FileChunkSaver(Paths.get(ConfigSingleton.getInstance().getStorageDir()));
    }

    /**
     * Callback для обновления списка файлов на сервере
     *
     * @param action Consumer<List<String>>
     */
    public void setFileListToServerActionUI(Consumer<List<String>> action) {
        callbackFileList = action;
    }

    /**
     * Callback для обновления списка файлов в локальном репозитории
     *
     * @param action Runnable
     */
    public void setFileListToLocalActionUI(Runnable action) {
        callbackFileData = action;
    }


    @Override
    public void run() {
        try {
            while (!currentThread().isInterrupted()) {
                DataPackage response = netConnection.getResponseFromServer();
                parsingResponse(response);
            }
        } catch (NetConnection.ServerResponseException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод для разбора ответа сервера
     */
    private void parsingResponse(DataPackage response)
            throws IOException {
        if (response instanceof FileListCommand) {
            FileListCommand com = (FileListCommand) response;
            callbackFileList.accept(com.getFileNames());
            return;
        }

        if (response instanceof FileDataPackage) {
            FileDataPackage pack = (FileDataPackage) response;
            Path path = Paths.get(ConfigSingleton.getInstance().getStorageDir() + "/" + pack.getFilename());
            Files.write(path, pack.getData());
            callbackFileData.run();
            return;
        }

        if (response instanceof FileChunkPackage) {
            saver.writeFileChunk((FileChunkPackage) response,
                    () -> callbackFileData.run());
        }
    }

}