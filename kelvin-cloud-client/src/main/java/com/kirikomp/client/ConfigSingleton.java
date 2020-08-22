package com.kirikomp.client;

public final class ConfigSingleton {

    private static ConfigSingleton instance;

    private String host;
    private int port;
    private int maxObjSize;
    private String storageDir;

    private ConfigSingleton() {
    }

    public static ConfigSingleton getInstance() {
        if (instance == null) {
            instance = new ConfigSingleton();
        }
        return instance;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxObjSize() {
        return maxObjSize;
    }

    public void setMaxObjSize(int maxObjSize) {
        this.maxObjSize = maxObjSize;
    }

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }
}