package com.kirikomp.client;


import com.kirikomp.common.AuthResult;
import com.kirikomp.common.DataPackage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.kirikomp.client.UIutils.*;
import static com.kirikomp.common.AuthResult.Result.OK;
import static java.lang.System.exit;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toList;


public class Controller
        implements Initializable {

    private NetConnection conn;
    private Window parentWin;
    private ServerResponseHandler responseHandler;
    private FileSender fileSender;
    private ExecutorService exec;
    private Runnable authHandler;

    private static final int THREAD_MAX_COUNT = 2;

    @FXML
    HBox rootElem;

    @FXML
    VBox vboxLoginPanel;
    @FXML
    TextField login;
    @FXML
    PasswordField password;

    @FXML
    VBox vboxLocalFiles;
    @FXML
    Button btnSendFiles;
    @FXML
    Button btnDeleteLocalFiles;
    @FXML
    ListView<String> lstLocalFiles;
    @FXML
    Button btnConn;
    @FXML
    Button btnDisconn;
    @FXML
    Button btnExit;

    @FXML
    VBox vboxServerFiles;
    @FXML
    Button btnDownloadFiles;
    @FXML
    Button btnDeleteFilesInCloud;
    @FXML
    ListView<String> lstFilesInCloud;


    public void auth(ActionEvent event) {
        try {
            conn.auth(login.getText(), password.getText());
        } catch (NetConnection.SendDataException e) {
            showError(e.getCause().getMessage());
        }
    }


    private class AuthHandler
            implements Runnable {

        @Override
        public void run() {
            try {
                while (!currentThread().isInterrupted()) {
                    DataPackage response = conn.getResponseFromServer();

                    if (response instanceof AuthResult) {
                        AuthResult ar = (AuthResult) response;
                        if (ar.getResult() == OK)
                            break;

                        showError("Ошибка авторизации");
                    }
                }
                initMainPanel();
            } catch (NetConnection.ServerResponseException e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        conn = App.getNetConnection();

        vboxLocalFiles.setVisible(false);
        vboxLocalFiles.setManaged(false);

        vboxServerFiles.setVisible(false);
        vboxServerFiles.setManaged(false);

        authHandler = new Controller.AuthHandler();
        exec = Executors.newSingleThreadExecutor();
        exec.execute(authHandler);
    }


    private void initMainPanel() {

        vboxLoginPanel.setVisible(false);
        vboxLoginPanel.setManaged(false);

        vboxLocalFiles.setVisible(true);
        vboxLocalFiles.setManaged(true);

        vboxServerFiles.setVisible(true);
        vboxServerFiles.setManaged(true);

        responseHandler = new ServerResponseHandler();
        responseHandler.setFileListActionUI(this::updateFileListFromCloud);
        responseHandler.setFileDataActionUI(this::updateLocalFileList);
        fileSender = new FileSender();
        exec = newFixedThreadPool(THREAD_MAX_COUNT);
        exec.execute(responseHandler);
        exec.execute(fileSender);

        btnConn.managedProperty().bind(btnConn.visibleProperty());
        btnDisconn.managedProperty().bind(btnDisconn.visibleProperty());
        btnConn.setVisible(false);
        btnConn.setDisable(true);

//        App.setStageTitle("");


        updateLocalFileList();
        getFileListFromCloud();
    }


    public void connectToCloud(ActionEvent event) {
        try {
            conn.open();
            exec.execute(responseHandler);
            exec.execute(fileSender);
            setButtonsState(true);
        } catch (IOException e) {
            showError("Не удалось подключиться");
        }
    }


    public void disconnectFromCloud(ActionEvent event) {
        try {
            conn.close();
            setButtonsState(false);
        } catch (IOException e) {
            showError("Ошибка обрыва связи");
        }
    }


    private void setButtonsState(boolean connected) {
        btnConn.setVisible(!connected);
        btnConn.setDisable(connected);
        btnDisconn.setDisable(!connected);
        btnDisconn.setVisible(connected);

        btnSendFiles.setDisable(!connected);
        btnDownloadFiles.setDisable(!connected);
        btnDeleteFilesInCloud.setDisable(!connected);
    }


    public void getFileListFromCloud() {
        try {
            conn.sendFileListCommand();
        } catch (NetConnection.SendDataException e) {
            showError(e);
        }
    }


    public void sendFiles(ActionEvent event) {
        if (parentWin == null) parentWin = rootElem.getScene().getWindow();

        FileChooser fc = new FileChooser();
        List<File> files = fc.showOpenMultipleDialog(parentWin);

        if (files == null || files.isEmpty())
            return;

        fileSender.addFiles(files);
    }


    public void deleteLocalFiles(ActionEvent event) {
        try {
            MultipleSelectionModel<String> model = lstLocalFiles.getSelectionModel();
            List<String> items = model.getSelectedItems();

            for (String fn : items) {
                Path path = Paths.get(ConfigSingleton.getInstance().STORAGE_DIR + "/" + fn);
                Files.delete(path);
                updateLocalFileList();
            }
        } catch (IOException e) {
            showError(e);
        }
    }


    public void downloadFiles(ActionEvent event) {
        try {
            MultipleSelectionModel<String> model = lstFilesInCloud.getSelectionModel();
            List<String> items = model.getSelectedItems();
            conn.sendDownloadFilesCommand(items);
        } catch (NetConnection.SendDataException e) {
            showError(e);
        }
    }


    public void deleteFilesInCloud(ActionEvent event) {
        try {
            MultipleSelectionModel<String> model = lstFilesInCloud.getSelectionModel();
            List<String> items = model.getSelectedItems();
            conn.sendDeleteFilesCommand(items);
        } catch (NetConnection.SendDataException e) {
            showError(e);
        }
    }


    public void updateFileListFromCloud(List<String> filenames) {
        updateUI(() ->
        {
            List<String> items = lstFilesInCloud.getItems();
            items.clear();
            items.addAll(filenames);
        });
    }


    public void updateLocalFileList() {
        updateUI(() ->
        {
            try {
                List<String> fnames = Files.list(Paths.get(ConfigSingleton.getInstance().STORAGE_DIR))
                        .map(x -> x.getFileName().toString())
                        .collect(toList());

                List<String> items = lstLocalFiles.getItems();
                items.clear();
                items.addAll(fnames);
            } catch (IOException e) {
                showError(e);
            }
        });
    }


    public void exitFromApp(ActionEvent event) {
        exit(0);
    }

}