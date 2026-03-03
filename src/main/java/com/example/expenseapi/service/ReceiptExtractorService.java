package com.example.expenseapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.util.Base64;
import java.util.Map;

@Service
public class ReceiptExtractorService {

    @Value("${anthropic.api-key}")
    private String apiKey;
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL   = "claude-opus-4-6";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public ExtractedExpense extract(byte[] imageBytes, String mediaType) {
        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            Map<String, Object> imageSource = Map.of(
                "type",       "base64",
                "media_type", mediaType,
                "data",       base64
            );

            Map<String, Object> imageContent = Map.of(
                "type",   "image",
                "source", imageSource
            );

            Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", """
                    You are a receipt parser. Look at this receipt image and extract the expense details.
                    Respond with ONLY a JSON object — no markdown, no explanation, no backticks.
                    The JSON must have exactly these three fields:
                    {
                      "title": "<merchant name or brief description of purchase, max 60 chars>",
                      "amount": <total amount as a plain number, no currency symbols>,
                      "category": "<one of: Food, Transport, Shopping, Utilities, Health, Entertainment, Other>"
                    }
                    If you cannot determine a value, use null for amount and "Other" for category.
                    """
            );

            Map<String, Object> message = Map.of(
                "role",    "user",
                "content", new Object[]{ imageContent, textContent }
            );

            Map<String, Object> body = Map.of(
                "model",      MODEL,
                "max_tokens", 256,
                "messages",   new Object[]{ message }
            );

            String bodyJson = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type",      "application/json")
                .header("x-api-key",         apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Claude API error " + response.statusCode() + ": " + response.body());
            }
            JsonNode root    = mapper.readTree(response.body());
            String rawText   = root.path("content").get(0).path("text").asText();
            JsonNode parsed  = mapper.readTree(rawText.trim());

            String title     = parsed.path("title").asText("Unknown");
            Double amount    = parsed.path("amount").isNull() ? null : parsed.path("amount").asDouble();
            String category  = parsed.path("category").asText("Other");

            return new ExtractedExpense(title, amount, category);

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract receipt details: " + e.getMessage(), e);
        }
    }
    public record ExtractedExpense(String title, Double amount, String category) {}
}