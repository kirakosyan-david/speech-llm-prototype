package org.example.speechllmprototype;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LLMService {

    private static final String OPENAI_API_KEY = "YOUR_API_KEY";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static String callLLM(String recognizedText) {
        if (OPENAI_API_KEY == null || OPENAI_API_KEY.isBlank()) {
            return "ERROR: OPENAI_API_KEY not set";
        }
        try {
            String json = """
                    {
                    "model": "gpt-3.5-turbo",
                    "messages": [{"role": "user", "content": "%s"}]
                    "max_tokens": 200
                    }
                    """.formatted(recognizedText);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + OPENAI_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            int index = body.indexOf("\"content\":\"");
            if (index != -1) {
                int colon = body.indexOf(':', index);
                if (colon != -1) {
                    int firstQuote = body.indexOf('"', colon + 1);
                    if (firstQuote != -1) {
                        int start = body.indexOf('"', firstQuote + 1);
                        if (start != -1) {
                            int end = body.indexOf('"', start + 1);
                            if (end != -1) {
                                String raw = body.substring(start + 1, end);
                                return raw
                                        .replace("\\n", "\n")
                                        .replace("\\\"", "\"")
                                        .replace("\\\\", "\\");
                            }
                        }
                    }
                }
            }
            return body.isBlank() ? recognizedText : body;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "LLM ERROR: " + e.getMessage();
        }
    }
}
