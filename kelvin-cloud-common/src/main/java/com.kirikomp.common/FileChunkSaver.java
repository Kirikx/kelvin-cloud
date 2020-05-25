package com.kirikomp.common;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class FileChunkSaver {

    private final Path dir;
    private String filename;
    private OutputStream out;


    public FileChunkSaver(Path dir) {
        this.dir = dir;
    }


    public void writeFileChunk(FileChunkPackage pack, Runnable saveFullAction)
            throws IOException {
        if (filename == null)
            filename = pack.getFilename();

        if (pack.isFirst()) {
            Path path = dir.resolve(filename);
            out = Files.newOutputStream(path);
        }

        out.write(pack.getData());

        if (pack.isLast()) {
            out.flush();
            out.close();
            filename = null;
            saveFullAction.run();
        }
    }

}