package com.kirikomp.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static io.netty.channel.ChannelOption.SO_BACKLOG;
import static io.netty.channel.ChannelOption.SO_KEEPALIVE;
import static io.netty.handler.codec.serialization.ClassResolvers.cacheDisabled;

public class Server {

    private static int PORT;
    protected static String STORAGE_DIR;
    private static int MAX_OBJ_SIZE;


    public static void main(String[] args) throws Exception {
        Properties property = new Properties();
        Server server = new Server();

        Path path = Paths.get("kelvin-cloud-server", "src", "main", "resources", "config.properties");

        try (FileInputStream fis = new FileInputStream(path.toString())) {
            property.load(fis);
            // если порт не указан в качестве входящего параметра в аргументах, берем из файла конфигурации
            PORT = args.length > 0 ? Integer.parseInt(args[0]): Integer.parseInt(property.getProperty("port", "1234"));
            STORAGE_DIR = property.getProperty("storage.dir", "server/server_storage");
            MAX_OBJ_SIZE = Integer.parseInt(property.getProperty("max.obj.size", "52428800"));

            System.out.println("PORT: " + PORT
                    + ", STORAGE_DIR: " + STORAGE_DIR
                    + ", MAX_OBJ_SIZE: " + MAX_OBJ_SIZE);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения файла 'config.properties'!!! " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Неверный формат настроек сервера 'config.properties'!!! " + e.getMessage());
        }
        server.run();
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(new ObjectDecoder(MAX_OBJ_SIZE, cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new AuthHandler(),
                                    new MainHandler()
                                    );
                        }
                    }).option(SO_BACKLOG, 128)
                    .childOption(SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(PORT).sync();
            System.out.println("Server run!");
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}
