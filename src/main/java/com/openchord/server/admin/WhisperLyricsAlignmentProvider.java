package com.openchord.server.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openchord.server.config.OpenChordProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

/** Transcribes audio with whisper.cpp and aligns its timestamped words to authoritative lyrics. */
@Component
public class WhisperLyricsAlignmentProvider implements LyricsAlignmentProvider {
    private final String endpoint;
    private final ObjectMapper mapper;
    private final ForcedLyricsAligner aligner = new ForcedLyricsAligner();
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public WhisperLyricsAlignmentProvider(OpenChordProperties properties, ObjectMapper mapper) {
        this.endpoint = properties.lyricsAlignerUrl();
        this.mapper = mapper;
    }

    @Override
    public boolean isAvailable() {
        return !endpoint.isBlank();
    }

    @Override
    public AlignmentResult align(Path audio, String sourceText, long durationMs) throws Exception {
        if (!isAvailable()) throw new IllegalStateException("Lyrics alignment engine is not configured");
        String boundary = "OpenChord-" + UUID.randomUUID();
        HttpRequest.BodyPublisher body = multipart(audio, boundary);
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(endpoint + "/inference"))
                        .timeout(Duration.ofHours(1))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(body)
                        .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("whisper.cpp returned HTTP " + response.statusCode());
        }
        List<ForcedLyricsAligner.RecognizedWord> words = recognizedWords(mapper.readTree(response.body()));
        return aligner.align(sourceText, words, "whisper.cpp", durationMs);
    }

    private static HttpRequest.BodyPublisher multipart(Path audio, String boundary) throws Exception {
        String fields =
                part(boundary, "response_format", "verbose_json")
                        + part(boundary, "word_timestamps", "true")
                        + part(boundary, "language", "auto")
                        + part(boundary, "temperature", "0.0")
                        + "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"audio"
                        + extension(audio) + "\"\r\n"
                        + "Content-Type: application/octet-stream\r\n\r\n";
        return HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(fields, StandardCharsets.UTF_8),
                HttpRequest.BodyPublishers.ofFile(audio),
                HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n"));
    }

    private static String part(String boundary, String name, String value) {
        return "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
    }

    private static String extension(Path audio) {
        String filename = audio.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot).replaceAll("[^A-Za-z0-9.]", "");
    }

    private static List<ForcedLyricsAligner.RecognizedWord> recognizedWords(JsonNode root) {
        List<ForcedLyricsAligner.RecognizedWord> result = new ArrayList<>();
        for (JsonNode segment : root.path("segments")) {
            JsonNode words = segment.path("words");
            if (words.isArray() && !words.isEmpty() && words.get(0).has("start")) {
                for (JsonNode word : words) {
                    addWord(
                            result,
                            word.path("word").asText(),
                            word.path("start").asDouble(),
                            word.path("end").asDouble(),
                            word.path("probability").asDouble(0.5));
                }
            } else {
                interpolateSegment(result, segment);
            }
        }
        return result;
    }

    private static void interpolateSegment(
            List<ForcedLyricsAligner.RecognizedWord> result, JsonNode segment) {
        String[] tokens = segment.path("text").asText().strip().split("\\s+");
        if (tokens.length == 0 || tokens[0].isBlank()) return;
        double start = segment.path("start").asDouble();
        double end = segment.path("end").asDouble(start + 0.1);
        double step = Math.max(0.001, (end - start) / tokens.length);
        for (int index = 0; index < tokens.length; index++) {
            addWord(result, tokens[index], start + step * index, start + step * (index + 1), 0.5);
        }
    }

    private static void addWord(
            List<ForcedLyricsAligner.RecognizedWord> result,
            String text,
            double startSeconds,
            double endSeconds,
            double probability) {
        if (ForcedLyricsAligner.normalize(text).isEmpty()) return;
        long startMs = Math.max(0, Math.round(startSeconds * 1_000));
        result.add(
                new ForcedLyricsAligner.RecognizedWord(
                        text,
                        startMs,
                        Math.max(startMs + 1, Math.round(endSeconds * 1_000)),
                        (float) Math.max(0, Math.min(1, probability))));
    }
}
