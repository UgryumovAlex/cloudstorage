package com.geekbrains.cloudstorage.client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PanelController implements Initializable {

    private final Path initPath = Paths.get(".");

    @FXML
    public TableView<FileInfo> clientTable;

    @FXML
    public Label panelLabel;

    @FXML
    public ComboBox<String> driveBox;

    @FXML
    public TextField pathField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileType().getName()));
        fileTypeColumn.setPrefWidth(30);

        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileNameColumn.setPrefWidth(300);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(50);
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[Dir]";
                        }
                        setText(text);
                    }
                }
            };

        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("modified");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileSizeColumn.setPrefWidth(50);

        clientTable.getColumns().add(fileTypeColumn);
        clientTable.getColumns().add(fileNameColumn);
        clientTable.getColumns().add(fileSizeColumn);
        clientTable.getColumns().add(fileDateColumn);
        clientTable.getSortOrder().add(fileTypeColumn);

        driveBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            driveBox.getItems().add(p.toString());
        }
        driveBox.getSelectionModel().select(initPath.toAbsolutePath().getRoot().toString());

        clientTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path path = Paths.get(pathField.getText()).resolve(clientTable.getSelectionModel().getSelectedItem().getFileName());
                    if (Files.isDirectory(path)) {
                        getPathFiles(path);
                    }
                }
            }
        });

        clientTable.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    Path path = Paths.get(pathField.getText()).resolve(clientTable.getSelectionModel().getSelectedItem().getFileName());
                    if (Files.isDirectory(path)) {
                        getPathFiles(path);
                    }
                }
            }
        });

        getPathFiles(initPath);
    }

    public void getPathFiles(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            clientTable.getItems().clear();
            clientTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            clientTable.sort();
            clientTable.requestFocus();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Unable to get file's list", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            getPathFiles(upperPath);
        }
    }

    public void selectDriveAction(ActionEvent actionEvent) {
        ComboBox<String> cb = (ComboBox<String>) actionEvent.getSource();
        getPathFiles(Paths.get(cb.getSelectionModel().getSelectedItem()));
    }

    public void setPanelLabel(String panelInfo) {
        panelLabel.setText(panelInfo);
    }
}
