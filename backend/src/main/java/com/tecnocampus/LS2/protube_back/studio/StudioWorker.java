package com.tecnocampus.LS2.protube_back.studio;

import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;

@Component
@EnableScheduling
public class StudioWorker {
    private final StudioVideos videos;
    private final VideoProcessor processor;
    private final TransactionTemplate transaction;
    private volatile boolean started;

    public StudioWorker(StudioVideos videos, VideoProcessor processor, TransactionTemplate transaction) {
        this.videos = videos; this.processor = processor; this.transaction = transaction;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        transaction.executeWithoutResult(status -> videos.recover(StudioVideo.Status.PROCESSING, StudioVideo.Status.QUEUED));
        started = true;
    }

    @Scheduled(fixedDelay = 2000)
    public void processNext() {
        if (!started) return;
        var video = transaction.execute(status -> videos.findFirstByStatusOrderByCreatedAtAsc(StudioVideo.Status.QUEUED)
                .flatMap(candidate -> videos.lock(candidate.id))
                .filter(candidate -> candidate.status == StudioVideo.Status.QUEUED)
                .map(candidate -> { candidate.status = StudioVideo.Status.PROCESSING; return candidate; }).orElse(null));
        if (video == null) return;
        double duration = 0;
        String failure = null;
        try {
            duration = processor.process(video);
        } catch (VideoProcessor.InvalidVideo ex) {
            failure = ex.getMessage();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return; // The next startup recovers the queued work.
        } catch (Exception ex) {
            LoggerFactory.getLogger(StudioWorker.class).warn("Error processing video {}", video.id, ex);
            failure = "No se pudo procesar el vídeo. Revisa el archivo y vuelve a subirlo.";
        }
        double result = duration;
        String error = failure;
        transaction.executeWithoutResult(status -> videos.lock(video.id).ifPresent(current -> {
            current.duration = result;
            current.failure = error;
            current.finishedAt = Instant.now();
            current.status = error == null ? StudioVideo.Status.READY : StudioVideo.Status.ERROR;
        }));
    }
}
