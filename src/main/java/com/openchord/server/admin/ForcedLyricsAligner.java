package com.openchord.server.admin;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Fuzzy sequence alignment between authoritative lyrics and timestamped ASR words. */
final class ForcedLyricsAligner {
    LyricsAlignmentProvider.AlignmentResult align(
            String sourceText, List<RecognizedWord> recognized, String engine, long durationMs) {
        List<SourceLine> sourceLines = sourceLines(sourceText);
        List<SourceWord> sourceWords = sourceLines.stream().flatMap(line -> line.words().stream()).toList();
        if (sourceWords.isEmpty()) throw new IllegalArgumentException("Lyrics source is empty");
        if (recognized.isEmpty()) throw new IllegalArgumentException("Speech model returned no words");

        int sourceCount = sourceWords.size();
        int recognizedCount = recognized.size();
        double[][] cost = new double[sourceCount + 1][recognizedCount + 1];
        Direction[][] direction = new Direction[sourceCount + 1][recognizedCount + 1];
        for (int i = 1; i <= sourceCount; i++) {
            cost[i][0] = i;
            direction[i][0] = Direction.SKIP_SOURCE;
        }
        for (int j = 1; j <= recognizedCount; j++) {
            cost[0][j] = j;
            direction[0][j] = Direction.SKIP_RECOGNIZED;
        }
        for (int i = 1; i <= sourceCount; i++) {
            for (int j = 1; j <= recognizedCount; j++) {
                double similarity = similarity(sourceWords.get(i - 1).normalized(), recognized.get(j - 1).normalized());
                double match = cost[i - 1][j - 1] + (1 - similarity);
                double skipSource = cost[i - 1][j] + 0.9;
                double skipRecognized = cost[i][j - 1] + 0.9;
                if (match <= skipSource && match <= skipRecognized) {
                    cost[i][j] = match;
                    direction[i][j] = Direction.MATCH;
                } else if (skipSource <= skipRecognized) {
                    cost[i][j] = skipSource;
                    direction[i][j] = Direction.SKIP_SOURCE;
                } else {
                    cost[i][j] = skipRecognized;
                    direction[i][j] = Direction.SKIP_RECOGNIZED;
                }
            }
        }

        List<Match> matches = new ArrayList<>();
        int i = sourceCount;
        int j = recognizedCount;
        while (i > 0 || j > 0) {
            Direction step = direction[i][j];
            if (step == Direction.MATCH) {
                double score = similarity(sourceWords.get(i - 1).normalized(), recognized.get(j - 1).normalized());
                if (score >= 0.45) matches.add(new Match(i - 1, j - 1, score));
                i--;
                j--;
            } else if (step == Direction.SKIP_SOURCE) {
                i--;
            } else {
                j--;
            }
        }
        Collections.reverse(matches);

        List<LyricsAlignmentProvider.AlignedLine> lines = new ArrayList<>();
        long previousStart = -1;
        for (SourceLine sourceLine : sourceLines) {
            List<Match> lineMatches =
                    matches.stream()
                            .filter(match -> sourceWords.get(match.sourceIndex()).lineIndex() == sourceLine.index())
                            .toList();
            if (lineMatches.isEmpty()) continue;
            RecognizedWord first = recognized.get(lineMatches.getFirst().recognizedIndex());
            RecognizedWord last = recognized.get(lineMatches.getLast().recognizedIndex());
            float coverage = (float) lineMatches.size() / sourceLine.words().size();
            float lexical =
                    (float) lineMatches.stream().mapToDouble(Match::similarity).average().orElse(0);
            float confidence = clamp(coverage * lexical * first.probabilityAverage(lineMatches, recognized));
            long startMs = Math.max(first.startMs(), previousStart + 1);
            if (startMs >= durationMs) continue;
            long endMs = Math.min(durationMs, Math.max(startMs + 1, last.endMs()));
            lines.add(
                    new LyricsAlignmentProvider.AlignedLine(
                            sourceLine.text(), startMs, endMs, confidence));
            previousStart = startMs;
        }
        if (lines.isEmpty()) throw new IllegalArgumentException("Lyrics could not be aligned to transcription");
        float average =
                (float) lines.stream()
                        .mapToDouble(LyricsAlignmentProvider.AlignedLine::confidence)
                        .average()
                        .orElse(0);
        return new LyricsAlignmentProvider.AlignmentResult(engine, clamp(average), lines);
    }

    private static List<SourceLine> sourceLines(String source) {
        List<SourceLine> result = new ArrayList<>();
        int wordIndex = 0;
        for (String rawLine : source.replace("\r", "").split("\n")) {
            String text = rawLine.strip();
            if (text.isEmpty() || text.matches("^\\[.*]$")) continue;
            int lineIndex = result.size();
            List<SourceWord> words = new ArrayList<>();
            for (String token : text.split("\\s+")) {
                String normalized = normalize(token);
                if (!normalized.isEmpty()) words.add(new SourceWord(wordIndex++, lineIndex, normalized));
            }
            if (!words.isEmpty()) result.add(new SourceLine(lineIndex, text, List.copyOf(words)));
        }
        return result;
    }

    static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static double similarity(String left, String right) {
        if (left.equals(right)) return 1;
        int longest = Math.max(left.length(), right.length());
        if (longest == 0) return 1;
        int[] previous = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) previous[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int[] current = new int[right.length() + 1];
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int substitution = previous[j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            previous = current;
        }
        return Math.max(0, 1d - (double) previous[right.length()] / longest);
    }

    private static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }

    record RecognizedWord(String text, long startMs, long endMs, float probability) {
        String normalized() {
            return normalize(text);
        }

        float probabilityAverage(List<Match> matches, List<RecognizedWord> recognized) {
            return clamp(
                    (float) matches.stream()
                            .mapToDouble(match -> recognized.get(match.recognizedIndex()).probability())
                            .average()
                            .orElse(probability));
        }
    }

    private record SourceLine(int index, String text, List<SourceWord> words) {
    }

    private record SourceWord(int index, int lineIndex, String normalized) {
    }

    private record Match(int sourceIndex, int recognizedIndex, double similarity) {
    }

    private enum Direction {
        MATCH,
        SKIP_SOURCE,
        SKIP_RECOGNIZED
    }
}
