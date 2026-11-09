package com.tecnocampus.LS2.protube_back.studio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.auth.*;
import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.SpringDataUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

@Service
public class StudioService {
    static final int CHUNK_SIZE = 2 * 1024 * 1024;
    static final int IMAGE_SIZE = 5 * 1024 * 1024;
    private final StudioVideos videos;
    private final StudioStorage storage;
    private final AuthAccounts accounts;
    private final SpringDataUserRepository users;
    private final ObjectMapper json;
    private final long maximumBytes;
    private final int maximumSeconds;

    public record Start(String filename, long bytes, String checksum, VideoMetadata metadata) {}
    public record Limits(long maxBytes, int maxSeconds, int chunkBytes, int imageBytes) {}
    public record Item(String id, String filename, String checksum, long totalBytes, long receivedBytes,
                       StudioVideo.Status status, double duration, String failure, Instant finishedAt,
                       VideoMetadata metadata, List<String> thumbnails) {}
    public record Playback(String id, String title, String description, String channel, List<String> tags,
                           String category, String language, boolean ageRestricted, boolean commentsAllowed, double duration, String thumbnail) {}
    public record Listing(String id, String title, String channel, String views, String published, String duration,
                          String categoryId, String thumbnail, long viewCount, String createdAt) {}

    public StudioService(StudioVideos videos, StudioStorage storage, AuthAccounts accounts, SpringDataUserRepository users,
                         ObjectMapper json, @Value("${protube.studio.max-bytes:104857600}") long maximumBytes,
                         @Value("${protube.studio.max-seconds:1200}") int maximumSeconds) {
        this.videos = videos; this.storage = storage; this.accounts = accounts; this.users = users; this.json = json;
        this.maximumBytes = maximumBytes; this.maximumSeconds = maximumSeconds;
    }

    Limits limits() { return new Limits(maximumBytes, maximumSeconds, CHUNK_SIZE, IMAGE_SIZE); }

    @Transactional
    Item start(Long owner, Start request) {
        if (request.filename == null || request.filename.length() > 255
                || !request.filename.toLowerCase(Locale.ROOT).matches("[^/\\\\]+\\.(mp4|mov|webm)"))
            throw error(400, "Selecciona un archivo MP4, MOV o WebM.");
        if (request.bytes <= 0 || request.bytes > maximumBytes)
            throw error(400, "El archivo supera el tamaño permitido o está vacío.");
        if (request.checksum == null || !request.checksum.matches("[0-9a-f]{64}"))
            throw error(400, "No se pudo identificar el archivo.");
        var metadata = validate(request.metadata);
        var existing = videos.findFirstByOwnerIdAndChecksumAndStatus(owner, request.checksum, StudioVideo.Status.UPLOADING);
        if (existing.isPresent() && existing.get().totalBytes == request.bytes) return item(existing.get());
        var video = new StudioVideo();
        video.id = UUID.randomUUID().toString(); video.ownerId = owner; video.filename = request.filename;
        video.totalBytes = request.bytes; video.checksum = request.checksum; video.status = StudioVideo.Status.UPLOADING;
        video.createdAt = Instant.now(); video.metadata = encode(metadata);
        if (metadata.thumbnail().equals("custom")) throw error(400, "Sube primero la miniatura personalizada.");
        return item(videos.save(video));
    }

    List<Item> list(Long owner) {
        return videos.findByOwnerIdAndStatusNotOrderByCreatedAtDesc(owner, StudioVideo.Status.CANCELLED).stream().map(this::item).toList();
    }

    Item get(Long owner, String id) { return item(owned(owner, id, false)); }

    @Transactional(rollbackFor = IOException.class)
    Item chunk(Long owner, String id, long offset, byte[] bytes) throws IOException {
        var video = owned(owner, id, true);
        requireUploading(video);
        if (offset != video.receivedBytes) throw error(409, "El progreso cambió. Reanuda desde el último bloque recibido.");
        if (bytes.length == 0 || bytes.length > CHUNK_SIZE || offset + bytes.length > video.totalBytes)
            throw error(400, "El bloque no tiene un tamaño válido.");
        storage.append(id, offset, bytes);
        video.receivedBytes += bytes.length;
        return item(video);
    }

    @Transactional
    Item finish(Long owner, String id) {
        var video = owned(owner, id, true);
        if (Set.of(StudioVideo.Status.QUEUED, StudioVideo.Status.PROCESSING, StudioVideo.Status.READY).contains(video.status)) return item(video);
        requireUploading(video);
        if (video.receivedBytes != video.totalBytes) throw error(409, "Todavía faltan partes del archivo.");
        video.status = StudioVideo.Status.QUEUED;
        return item(video);
    }

    @Transactional(rollbackFor = IOException.class)
    void cancel(Long owner, String id) throws IOException {
        var video = owned(owner, id, true);
        if (!Set.of(StudioVideo.Status.UPLOADING, StudioVideo.Status.QUEUED, StudioVideo.Status.ERROR, StudioVideo.Status.CANCELLED).contains(video.status))
            throw error(409, "No puedes cancelar un vídeo que ya se está procesando o está listo.");
        storage.remove(id);
        video.status = StudioVideo.Status.CANCELLED;
    }

    @Transactional
    Item edit(Long owner, String id, VideoMetadata input) {
        var video = owned(owner, id, true);
        if (video.status == StudioVideo.Status.CANCELLED) throw error(404, "No se encontró el vídeo.");
        var metadata = validate(input);
        if (metadata.thumbnail().equals("custom") && !Files.isRegularFile(storage.directory(id).resolve("custom.jpg")))
            throw error(400, "Sube primero la miniatura personalizada.");
        video.metadata = encode(metadata);
        return item(video);
    }

    @Transactional(rollbackFor = IOException.class)
    Item thumbnail(Long owner, String id, byte[] bytes) throws IOException {
        var video = owned(owner, id, true);
        if (video.status == StudioVideo.Status.CANCELLED) throw error(404, "No se encontró el vídeo.");
        if (bytes.length == 0 || bytes.length > IMAGE_SIZE) throw error(400, "La miniatura debe pesar hasta 5 MB.");
        storage.thumbnail(id, bytes);
        return item(video);
    }

    public List<Listing> catalog(Long viewer) {
        return videos.findByStatusOrderByCreatedAtDesc(StudioVideo.Status.READY).stream().filter(video -> {
            var m = metadata(video);
            return m.visibility() == VideoMetadata.Visibility.PUBLIC && m.published(Instant.now()) && allowedAge(video, m, viewer);
        }).map(video -> {
            var m = metadata(video);
            long seconds = (long) video.duration;
            return new Listing(video.id, m.title(), channel(video), "0 visualizaciones", "",
                    "%d:%02d".formatted(seconds / 60, seconds % 60), m.category(),
                    "/api/studio/play/" + video.id + "/thumbnail", 0, video.createdAt.toString());
        }).toList();
    }

    Playback playback(Long viewer, String id) {
        var video = visible(viewer, id);
        var m = metadata(video);
        return new Playback(id, m.title(), m.description(), channel(video), m.tags(), m.category(), m.language(),
                m.ageRestricted(), m.commentsAllowed(), video.duration, m.thumbnail());
    }

    FileSystemResource publicFile(Long viewer, String id, String filename) {
        var video = visible(viewer, id);
        String name = filename.equals("thumbnail") ? metadata(video).thumbnail() + ".jpg" : filename;
        if (!Set.of("360.mp4", "720.mp4", "1080.mp4", metadata(video).thumbnail() + ".jpg").contains(name))
            throw error(404, "Archivo no disponible.");
        return resource(id, name);
    }

    FileSystemResource preview(Long owner, String id, String name) {
        var video = owned(owner, id, false);
        if (video.status == StudioVideo.Status.CANCELLED || !Set.of("auto-1.jpg", "auto-2.jpg", "auto-3.jpg", "custom.jpg").contains(name)
                || (!name.equals("custom.jpg") && video.status != StudioVideo.Status.READY))
            throw error(404, "Miniatura no disponible.");
        return resource(id, name);
    }

    private FileSystemResource resource(String id, String name) {
        Path path = storage.directory(id).resolve(name);
        if (!Files.isRegularFile(path)) throw error(404, "Archivo no disponible.");
        return new FileSystemResource(path);
    }

    private StudioVideo visible(Long viewer, String id) {
        var video = videos.findById(id).orElseThrow(() -> error(404, "Vídeo no disponible."));
        var m = metadata(video);
        if (video.status != StudioVideo.Status.READY || (!video.ownerId.equals(viewer)
                && (!m.published(Instant.now()) || m.visibility() == VideoMetadata.Visibility.PRIVATE || !allowedAge(video, m, viewer))))
            throw error(404, "Vídeo no disponible.");
        return video;
    }

    private boolean allowedAge(StudioVideo video, VideoMetadata metadata, Long viewer) {
        return !metadata.ageRestricted() || video.ownerId.equals(viewer) || (viewer != null && accounts.findById(viewer)
                .map(account -> account.birthDate != null && !account.birthDate.isAfter(LocalDate.now().minusYears(18))).orElse(false));
    }

    private String channel(StudioVideo video) {
        return users.findById(video.ownerId).map(user -> user.getUsername()).orElse("Canal");
    }

    private StudioVideo owned(Long owner, String id, boolean lock) {
        var video = (lock ? videos.lock(id) : videos.findById(id)).orElseThrow(() -> error(404, "No se encontró el vídeo."));
        if (!video.ownerId.equals(owner)) throw error(403, "Solo el propietario puede gestionar este vídeo.");
        return video;
    }

    private Item item(StudioVideo video) {
        var thumbnails = new ArrayList<String>();
        if (video.status == StudioVideo.Status.READY) thumbnails.addAll(List.of("auto-1", "auto-2", "auto-3"));
        if (Files.isRegularFile(storage.directory(video.id).resolve("custom.jpg"))) thumbnails.add("custom");
        return new Item(video.id, video.filename, video.checksum, video.totalBytes, video.receivedBytes,
                video.status, video.duration, video.failure, video.finishedAt, metadata(video), thumbnails);
    }

    private VideoMetadata validate(VideoMetadata metadata) {
        if (metadata == null) throw error(400, "Completa los datos del vídeo.");
        return metadata.validated();
    }

    private VideoMetadata metadata(StudioVideo video) {
        try { return json.readValue(video.metadata, VideoMetadata.class); }
        catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    private String encode(VideoMetadata metadata) {
        try { return json.writeValueAsString(metadata); }
        catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    private static void requireUploading(StudioVideo video) {
        if (video.status != StudioVideo.Status.UPLOADING) throw error(409, "Este vídeo ya no admite más datos.");
    }

    private static AuthFailure error(int status, String message) { return new AuthFailure(status, message); }
}
