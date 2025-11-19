package org.example.speechllmprototype;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class VoiceRecognizer {

    private final Consumer<String> callBack;
    private boolean running = false;
    private ExecutorService executorService;

    private Model model;
    private Recognizer recognizer;
    private TargetDataLine microphone;

    private static final String MODEL_PATH = "D:\\Test_Task\\speech-llm-prototype\\src\\main\\resources\\org\\example\\speechllmprototype\\vosk-model-small-ru-0.22";

    public VoiceRecognizer(Consumer<String> callBack) {
        this.callBack = callBack;

        try {
            File modelDir = new File(MODEL_PATH);
            if (!modelDir.exists() || !modelDir.isDirectory()) {
                System.err.println("[ASR] Model folder not found: " + MODEL_PATH);
            } else {
                model = new Model(MODEL_PATH);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean startRecognition() {
        if (model == null) {
            System.err.println("[ASR] Model is null, can't start recognition");
            return false;
        }
        running = true;
        executorService = Executors.newSingleThreadExecutor();
        try {
            executorService.submit(this::recognize);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            running = false;
            return false;
        }
    }

    public void stopRecognition() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (microphone != null) {
            try {
                microphone.stop();
                microphone.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (recognizer != null) {
            try {
                recognizer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void recognize() {
        AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
        try {
            microphone = getAvailableMicrophone(format);
            if (microphone == null){
                System.err.println("[ASR] No microphone available");
                running = false;
                return;
            }
            microphone.start();

            recognizer = new Recognizer(model, 16000.0f);
            byte[] buffer = new byte[4096];

            while (running) {
                int n = microphone.read(buffer, 0, buffer.length);
                if (n > 0) {
                    String result;
                    if (recognizer.acceptWaveForm(buffer, n)) {
                        result = recognizer.getResult();
                    } else {
                        result = recognizer.getPartialResult();
                    }
                    String text = extractTextFromVoskResult(result);
                    if (text != null && !text.isBlank()){
                        callBack.accept(text.trim());
                    }
//                    callBack.accept(result);
                }else {
                    Thread.sleep(10);
                }
            }

        } catch (LineUnavailableException | InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            if (microphone != null) {
                microphone.stop();
                microphone.close();
            }
            if (recognizer != null) {
                recognizer.close();
            }
        }
    }

    private String extractTextFromVoskResult(String json) {
        if (json == null) return null;
        int idxText = json.indexOf("\"text\"");
        if (idxText != -1) {
            int colon = json.indexOf(':', idxText);
            if (colon != -1) {
                int firstQuote = json.indexOf('"', colon + 1);
                int secondQuote = json.indexOf('"', firstQuote + 1);
                if (firstQuote != -1 && secondQuote != -1) {
                    return json.substring(firstQuote + 1, secondQuote);
                }
            }
        }
        int idxPartial = json.indexOf("\"partial\"");
        if (idxPartial != -1) {
            int colon = json.indexOf(':', idxPartial);
            if (colon != -1) {
                int firstQuote = json.indexOf('"', colon + 1);
                int secondQuote = json.indexOf('"', firstQuote + 1);
                if (firstQuote != -1 && secondQuote != -1) {
                    return json.substring(firstQuote + 1, secondQuote);
                }
            }
        }
        return null;
    }

    private TargetDataLine getAvailableMicrophone(AudioFormat format) throws LineUnavailableException {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        System.out.println("[ASR] Available mixers:");
        for (Mixer.Info info : mixers) {
            System.out.println("  " + info.getName());
        }
        for (Mixer.Info mixerInfo : mixers) {
            String name = mixerInfo.getName().toLowerCase();
            if (name.contains("microphone") || name.contains("mic") || name.contains("record")) {
                try {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    if (mixer.isLineSupported(info)) {
                        TargetDataLine line = (TargetDataLine) mixer.getLine(info);
                        line.open(format);
                        System.out.println("[ASR] Using mixer: " + mixerInfo.getName());
                        return line;
                    }
                } catch (LineUnavailableException ignored) {}
            }
        }
        for (Mixer.Info mixerInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (mixer.isLineSupported(info)) {
                    TargetDataLine line = (TargetDataLine) mixer.getLine(info);
                    line.open(format);
                    System.out.println("[ASR] Using fallback mixer: " + mixerInfo.getName());
                    return line;
                }
            } catch (LineUnavailableException ignored) {}
        }
        TargetDataLine defaultLine = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
        defaultLine.open(format);
        return defaultLine;
    }
}
