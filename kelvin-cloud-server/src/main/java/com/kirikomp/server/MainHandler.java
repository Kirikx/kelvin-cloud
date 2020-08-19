package com.kirikomp.server;

import com.kirikomp.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.stream.Collectors.toList;


public class MainHandler
        extends ChannelInboundHandlerAdapter {

    private Path userDir;
    private FileChunkSaver saver;
    Runnable action;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        action = () -> {
            try {
                MainHandler.this.sendFileList(ctx);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    public void setUserDir(Path dir) {
        userDir = dir;
        saver = new FileChunkSaver(dir);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        try {
            if (msg instanceof FileListCommand) {
                sendFileList(ctx, (FileListCommand) msg);
                return;
            }

            if (msg instanceof GetFilesCommand) {
                sendFiles(ctx, (GetFilesCommand) msg);
                return;
            }

            if (msg instanceof DeleteFilesCommand) {
                deleteFiles((DeleteFilesCommand) msg);
                sendFileList(ctx);
                return;
            }

            if (msg instanceof FileDataPackage) {
                saveFile((FileDataPackage) msg);
                sendFileList(ctx);
                return;
            }

            if (msg instanceof FileChunkPackage) {
                saver.writeFileChunk((FileChunkPackage) msg, action);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * Метод передачи списка файлов клиента на сервере
     *
     * @param ctx контекст
     * @throws IOException
     */
    private void sendFileList(ChannelHandlerContext ctx)
            throws IOException {
        sendFileList(ctx, new FileListCommand());
    }

    /**
     * Перегруженый метод передачи списка файлов клиента на сервере
     *
     * @param ctx контекст
     * @param com Команда FileListCommand
     * @throws IOException
     */
    private void sendFileList(ChannelHandlerContext ctx, FileListCommand com)
            throws IOException {
        List<String> fnames = Files.list(userDir)
                .map(x -> x.getFileName().toString())
                .collect(toList());

        com.setFileNames(fnames);
        ctx.writeAndFlush(com);
    }

    /**
     * Метод сохранения файлов
     *
     * @param pack FileDataPackage
     * @throws IOException
     */
    private void saveFile(FileDataPackage pack)
            throws IOException {
        Path path = userDir.resolve(pack.getFilename());
        Files.write(path, pack.getData());
    }

    /**
     * Метод отправки файлов
     *
     * @param ctx контекст
     * @param com команда GetFilesCommand
     * @throws Exception
     */
    private void sendFiles(ChannelHandlerContext ctx, GetFilesCommand com)
            throws Exception {
        for (String fn : com.getFileNames()) {
            Path path = userDir.resolve(fn);
            FileSendOptimizer.sendFile(path, ctx::writeAndFlush);
        }
    }

    /**
     * Метод удаления файлов
     *
     * @param com команда DeleteFilesCommand
     * @throws IOException
     */
    private void deleteFiles(DeleteFilesCommand com)
            throws IOException {
        for (String fn : com.getFileNames()) {
            Path path = userDir.resolve(fn);
            Files.delete(path);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


}