package com.kirikomp.client;

import com.kirikomp.common.FileSendOptimizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import static java.lang.Thread.currentThread;

@Component
public class FileSender
        implements Runnable {
    @Autowired
    private NetConnection netConnection;
    private final PriorityBlockingQueue<File> queue;

    private static final int MAX_COUNT = 100;

    /**
     * Конструктор
     */
    public FileSender() {
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
                                netConnection.sendToServer(dataPackage);
                            } catch (NetConnection.SendDataException e) {
                                e.printStackTrace();
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод добавления файлов в очередь
     *
     * @param files File list
     */
    public void addFilesToQueue(List<File> files) {
        files.forEach(queue::put);
    }

}