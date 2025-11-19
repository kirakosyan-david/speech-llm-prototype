package org.example.speechllmprototype;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceApplication extends Application {

    private VoiceRecognizer recognizer;
    private TextArea textArea;
    private Button startButton;
    private ExecutorService llmExecutor;

    @Override
    public void start(Stage stage) {
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setEditable(false);

        startButton = new Button("Start recording");
        startButton.setOnAction(e -> toggleRecording());

        VBox root = new VBox(10, textArea, startButton);
        Scene scene = new Scene(root, 600, 400);

        stage.setTitle("Voice-to-Text with LLM");
        stage.setScene(scene);
        stage.show();

        llmExecutor = Executors.newSingleThreadExecutor();

        recognizer = new VoiceRecognizer(recognizedText -> {
            llmExecutor.submit(() -> {
                String llmResult = LLMService.callLLM(recognizedText);
                Platform.runLater(() -> textArea.appendText("[LLM] " + llmResult + System.lineSeparator()));
            });
            Platform.runLater(() -> textArea.appendText("[ASR] " + recognizedText + System.lineSeparator()));
        });
    }

    private void toggleRecording() {
        if (recognizer.isRunning()) {
            recognizer.stopRecognition();
            startButton.setText("Start recording");
        } else {
            if (recognizer.startRecognition()){
                startButton.setText("Stop recording");
            }else {
                Platform.runLater(() -> textArea.appendText("[ERROR] Can't open microphone" + System.lineSeparator()));
            }
        }
    }

    @Override
    public void stop() throws Exception {
        if (recognizer != null && recognizer.isRunning()){
            recognizer.startRecognition();
        }
      if (llmExecutor != null && !llmExecutor.isShutdown()){
          llmExecutor.shutdownNow();
      }
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }


}