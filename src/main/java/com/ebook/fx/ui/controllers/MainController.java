/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebook.fx.ui.controllers;

import com.ebook.fx.MainApp;
import com.ebook.fx.ui.views.BookEditView;
import com.ebook.fx.core.command.LoadPdfCoverImageCommand;
import com.ebook.fx.core.model.Book;
import com.ebook.fx.core.task.ImportFileTask;
import com.ebook.fx.core.util.ImageCache;
import com.ebook.fx.ui.views.MainView;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 *
 * @author maykoone
 */
public class MainController {

    @FXML
    private TableView<Book> booksTable;
    private ObservableList<Book> books;
    @FXML
    private ListView<String> navigationList;
    @FXML
    private TextField searchBox;
    @FXML
    private ListView<String> bookIndexList;
    @FXML
    private ImageView bookCover;
    @FXML
    private ProgressBar progressBar;
    @Inject
    private Instance<BookEditView> bookEditViewSource;
    @Inject
    private MainView view;
    @Inject
    private MainApp application;
    @Inject
    private ImageCache imageCache;
    @FXML
    private ResourceBundle resources;
    @Inject
    private Logger logger;

    public MainController() {
        books = FXCollections.observableArrayList();
    }

    @FXML
    private void initialize() {
        navigationList.setItems(FXCollections.observableArrayList("Library",
                "Favorites", "Recents"));

        progressBar.setVisible(false);

        booksTable.setContextMenu(initBooksTableContextMenu());

        booksTable.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Book> observable, Book oldValue, Book newValue) -> {
            if (newValue != null) {
                if (newValue.getFilePath() != null) {
                    imageCache.getAsync(newValue.getFilePath()).thenAcceptAsync(bookCover::setImage);
                } else {
                    bookCover.setImage(LoadPdfCoverImageCommand.defaultImage);
                }
                bookIndexList.setItems(FXCollections.observableArrayList(newValue.getContents()));
            }
        });
    }

    private ContextMenu initBooksTableContextMenu() {
        MenuItem mEdit = new MenuItem(resources.getString("label.edit"));
        mEdit.setOnAction(e -> {
            editBookAction();
        });
        MenuItem mOpen = new MenuItem(resources.getString("label.open"));
        mOpen.setOnAction(e -> {
            EventQueue.invokeLater(() -> {
                try {
                    Desktop.getDesktop().open(new File(booksTable.getSelectionModel().getSelectedItem().getFilePath()));
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Open file fail.", ex);
                }
            });
        });
        ContextMenu tableMenu = new ContextMenu(mEdit, mOpen);
        return tableMenu;
    }

    @FXML
    private void importFolder(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File directory = chooser.showDialog(application.getPrimaryStage());

        if (directory != null) {
            //only pdf
            ImportFileTask importTask = new ImportFileTask(directory);

            Alert importInfo = new Alert(Alert.AlertType.CONFIRMATION);
            importInfo.setTitle(resources.getString("dialog.import.folder.title"));
            importInfo.setContentText(MessageFormat.format(resources.getString("dialog.import.folder.msg"), importTask.getNumberOfFiles()));
            Optional<ButtonType> option = importInfo.showAndWait();
            if (option.get().equals(ButtonType.OK)) {
                logger.log(Level.INFO, "Perform import");
                importTask.setOnSucceeded(e -> {
                    booksTable.getItems().addAll(importTask.getValue());
                    hideProgressBar();
                });
                showProgressBar(importTask);
                new Thread(importTask).start();
            } else {
                logger.log(Level.INFO, "Do nothing");
            }
        }

    }

    @FXML
    private void importFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf", "*.PDF"));
        File file = chooser.showOpenDialog(application.getPrimaryStage());

        if (file != null) {
            ImportFileTask task = new ImportFileTask(file);
            task.setOnSucceeded(e -> {
                booksTable.getItems().addAll(task.getValue());
                hideProgressBar();
            });
            showProgressBar(task);
            new Thread(task).start();
        }
    }

    @FXML
    private void aboutAction(ActionEvent event) {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle(resources.getString("dialog.about.title"));
        about.setContentText(resources.getString("dialog.about.msg"));
        about.showAndWait();

    }

    @FXML
    private void editBookAction() {
        Book selectedBook = booksTable.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            BookEditView bookEditView = bookEditViewSource.get();
            BookEditController bookEditController = bookEditView.getController();
            bookEditController.selectedBookProperty().bind(booksTable.getSelectionModel().selectedItemProperty());
            bookEditController.currentBookProperty().addListener((observable, oldBook, newBook) -> {
                if (newBook != null) {
                    booksTable.refresh();
                }
            });
            bookEditView.show();
        }
    }

    private void showProgressBar(Worker<?> task) {
        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.setVisible(true);
    }

    private void hideProgressBar() {
        progressBar.setVisible(false);
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
    }

    public void setApplication(MainApp application) {
        this.application = application;
    }

}