package com.kirikomp.client;

import com.kirikomp.common.DataPackage;
import com.kirikomp.common.FileSendOptimizer;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;

import static java.lang.Thread.currentThread;


public class FileSender
        implements Runnable {

    private final NetConnection conn;
    private final PriorityBlockingQueue<File> queue;

    private static final int MAX_COUNT = 100;


    public FileSender() {
        conn = App.getNetConnection();
        queue = new PriorityBlockingQueue<>(MAX_COUNT, Comparator.comparingLong(File::length));
    }

    @Override
    public void run() {
        try {
            while (!currentThread().isInterrupted()) {
                Path path = queue.take().toPath();

                FileSendOptimizer.sendFile(path,
                        dataPackage -> {
                            try {
                                conn.sendToServer(dataPackage);
                            } catch (NetConnection.SendDataException e) {
                                e.printStackTrace();
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Добавление файлов в очередь
    public void addFilesToQueue(List<File> files) {
        files.forEach(queue::put);
    }

}