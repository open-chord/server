package com.openchord.server.admin;

import java.nio.file.Path;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Runs model inference after the request transaction has committed. */
@Component
class LyricsAlignmentWorker {
    private final LyricsAlignmentProvider provider;
    private final LyricsAlignmentPersistence persistence;

    LyricsAlignmentWorker(LyricsAlignmentProvider provider, LyricsAlignmentPersistence persistence) {
        this.provider = provider;
        this.persistence = persistence;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void align(Requested event) {
        try {
            persistence.complete(
                    event.trackId(),
                    provider.align(event.audio(), event.sourceText(), event.durationMs()));
        } catch (Exception error) {
            persistence.fail(event.trackId(), safeMessage(error));
        }
    }

    private static String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    record Requested(UUID trackId, Path audio, String sourceText, long durationMs) {
    }
}
