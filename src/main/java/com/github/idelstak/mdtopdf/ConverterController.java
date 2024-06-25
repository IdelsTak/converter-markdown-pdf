package com.github.idelstak.mdtopdf;

import com.itextpdf.html2pdf.*;
import com.vladsch.flexmark.html.*;
import com.vladsch.flexmark.parser.*;
import javafx.collections.*;
import javafx.collections.ListChangeListener.*;
import javafx.concurrent.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import static javafx.application.Platform.*;

public class ConverterController {

    private static final String SELECT_TEXT = "Select markdown files to convert to PDF";
    private final ObservableList<File> selectedFiles;
    @FXML
    private Label selectedFilesLabel;
    @FXML
    private VBox progressBox;
    @FXML
    private Label progressTitleLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressMessageLabel;

    public ConverterController() {selectedFiles = FXCollections.observableArrayList();}

    @FXML
    private void initialize() {
        selectedFilesLabel.skinProperty().addListener(observable -> runLater(() -> {
            String text = selectedFilesLabel.getText();
            progressBox.setVisible(!Objects.equals(text, SELECT_TEXT));
        }));

        selectedFiles.addListener((Change<? extends File> change) -> {
            if (change.next()) {
                ObservableList<? extends File> files = change.getList();

                runLater(() -> {
                    int filesCount = setTitleMessage(files.size());

                    Service<Void> converterService = new Service<>() {
                        @Override
                        protected Task<Void> createTask() {
                            return new Task<>() {
                                @Override
                                protected Void call() throws Exception {
                                    updateProgress(-1, -1);
                                    updateTitle();

                                    for (int i = 0; i < filesCount; i++) {
                                        File markdownFile = files.get(i);

                                        System.out.println("markdownFile = " + markdownFile);

                                        updateMessage("Converting %s to PDF...".formatted(markdownFile.getName()));

                                        // Read the markdown file
                                        String markdown = new String(Files.readAllBytes(markdownFile.toPath()));

                                        // Convert Markdown to HTML
                                        Parser parser = Parser.builder().build();
                                        HtmlRenderer renderer = HtmlRenderer.builder().build();
                                        String html = renderer.render(parser.parse(markdown));

                                        // Create an InputStream from the HTML string
                                        byte[] buffer = html.getBytes(StandardCharsets.UTF_8);
                                        InputStream htmlStream = new ByteArrayInputStream(buffer);

                                        // Create a PDF document
                                        String pdfPath = markdownFile.getAbsolutePath().replace(".md", ".pdf");
                                        FileOutputStream pdfOutputStream = new FileOutputStream(pdfPath);

                                        // Use HtmlConverter to convert HTML stream to PDF
                                        HtmlConverter.convertToPdf(htmlStream, pdfOutputStream);

                                        updateProgress(i + 1, filesCount);
                                        updateMessage("Converted %s to PDF".formatted(markdownFile.getName()));
                                        updateTitle();
                                    }

                                    updateProgress(filesCount, filesCount);
                                    updateTitle("%d markdown files converted to PDF".formatted(filesCount));

                                    return null;
                                }

                                private void updateTitle() {
                                    runLater(() -> {
                                        int workDone = (int) getWorkDone();
                                        int totalWork = (int) getTotalWork();
                                        if (workDone > 0) {
                                            updateTitle("Converting %d/%d...".formatted(workDone, totalWork));
                                        } else {
                                            updateTitle("Converting...");
                                        }
                                    });
                                }
                            };
                        }
                    };

                    progressTitleLabel.textProperty().bind(converterService.titleProperty());
                    progressMessageLabel.textProperty().bind(converterService.messageProperty());
                    progressBar.progressProperty().bind(converterService.progressProperty());
                    progressBox.visibleProperty().bind(converterService.runningProperty());

                    converterService.setOnFailed(event -> {
                        System.out.println("service failed with exception = " + event.getSource().getException());
                    });
                    converterService.setOnSucceeded(event -> runLater(() -> setTitleMessage(0)));

                    System.out.println("starting converterService = " + converterService);

                    converterService.start();
                });
            }
        });
    }

    private int setTitleMessage(int filesCount) {
        String text = filesCount > 0 ? "%d files selected".formatted(filesCount) : SELECT_TEXT;
        selectedFilesLabel.setText(text);
        return filesCount;
    }

    @FXML
    private void selectFiles(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown Files", "*.md"));
        Window owner = ((Node) actionEvent.getSource()).getScene().getWindow();
        List<File> files = fileChooser.showOpenMultipleDialog(owner);

        if (files != null) {
            selectedFiles.setAll(files);
        }
    }

}