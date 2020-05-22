package com.kirikomp.client;


import com.kirikomp.common.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Thread.currentThread;


public class ServerResponseHandler
        implements Runnable {

    private final NetConnection conn;
    private Consumer<List<String>> callbackFileList;
    private Runnable callbackFileData;
    private FileChunkSaver saver;


    public ServerResponseHandler() {
        conn = App.getNetConnection();
        saver = new FileChunkSaver(Paths.get(ConfigSingleton.getInstance().STORAGE_DIR));
    }


    public void setFileListActionUI(Consumer<List<String>> action) {
        callbackFileList = action;
    }


    public void setFileDataActionUI(Runnable action) {
        callbackFileData = action;
    }


    @Override
    public void run() {
        try {
            while (!currentThread().isInterrupted()) {
                DataPackage response = conn.getResponseFromServer();
                processResponse(response);
            }
        } catch (NetConnection.ServerResponseException | IOException e) {
            e.printStackTrace();
        }
    }


    private void processResponse(DataPackage response)
            throws IOException {
        if (response instanceof FileListCommand) {
            FileListCommand com = (FileListCommand) response;
            callbackFileList.accept(com.getFileNames());
            return;
        }

        if (response instanceof FileDataPackage) {
            FileDataPackage pack = (FileDataPackage) response;
            Path path = Paths.get(ConfigSingleton.getInstance().STORAGE_DIR + "/" + pack.getFilename());
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