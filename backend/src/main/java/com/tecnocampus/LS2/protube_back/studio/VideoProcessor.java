package com.tecnocampus.LS2.protube_back.studio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class VideoProcessor {
    private final StudioStorage storage;
    private final ObjectMapper json;
    private final String ffmpeg, ffprobe;
    private final int maximumSeconds;

    public VideoProcessor(StudioStorage storage, ObjectMapper json,
                          @Value("${protube.studio.ffmpeg:ffmpeg}") String ffmpeg,
                          @Value("${protube.studio.ffprobe:ffprobe}") String ffprobe,
                          @Value("${protube.studio.max-seconds:1200}") int maximumSeconds) {
        this.storage = storage;
        this.json = json;
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
        this.maximumSeconds = maximumSeconds;
    }

    double process(StudioVideo video) throws IOException, InterruptedException {
        Path source = storage.source(video.id);
        if (Files.size(source) != video.totalBytes || !storage.checksum(source).equals(video.checksum))
            throw new InvalidVideo("El archivo recibido no coincide con el original. Vuelve a subirlo.");
        Path directory = storage.directory(video.id);
        String extension = video.filename.substring(video.filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String demuxers = extension.equals("webm") ? "matroska,webm" : "mov,mp4,m4a,3gp,3g2,mj2";
        var info = json.readTree(run(directory, ffprobe, "-v", "error", "-protocol_whitelist", "file,pipe",
                "-format_whitelist", demuxers, "-show_entries", "format=duration:stream=codec_type,width,height",
                "-of", "json", source.toString()));
        double duration = info.path("format").path("duration").asDouble(-1);
        if (!Double.isFinite(duration) || duration <= 0 || duration > maximumSeconds)
            throw new InvalidVideo("El vídeo tiene una duración no válida o supera el máximo de " + maximumSeconds + " segundos.");
        boolean playable = false;
        for (var stream : info.path("streams")) {
            if (stream.path("codec_type").asText().equals("video") && stream.path("width").asInt() > 0
                    && stream.path("width").asInt() <= 7680 && stream.path("height").asInt() > 0
                    && stream.path("height").asInt() <= 4320) playable = true;
        }
        if (!playable) throw new InvalidVideo("El archivo no contiene una pista de vídeo compatible.");
        if (extension.equals("webm")) {
            try (var input = Files.newInputStream(source)) {
                if (!new String(input.readNBytes(4096), StandardCharsets.ISO_8859_1).contains("webm"))
                    throw new InvalidVideo("El contenido del archivo no es WebM.");
            }
        }
        for (int height : new int[]{360, 720, 1080}) {
            int width = height * 16 / 9;
            run(directory, ffmpeg, "-nostdin", "-y", "-v", "error", "-xerror", "-err_detect", "explode",
                    "-protocol_whitelist", "file,pipe", "-i", source.toString(), "-map", "0:V:0", "-map", "0:a:0?",
                    "-vf", fit(width, height), "-c:v", "libx264", "-preset", "veryfast", "-threads", "2",
                    "-pix_fmt", "yuv420p", "-c:a", "aac", "-movflags", "+faststart",
                    directory.resolve(height + ".mp4").toString());
        }
        for (int frame = 1; frame <= 3; frame++) {
            Path thumbnail = directory.resolve("auto-" + frame + ".jpg");
            Files.deleteIfExists(thumbnail);
            run(directory, ffmpeg, "-nostdin", "-y", "-v", "error", "-ss",
                    Double.toString(duration * frame / 4), "-i", directory.resolve("360.mp4").toString(),
                    "-frames:v", "1", "-vf", fit(640, 360), "-threads", "1",
                    thumbnail.toString());
            if (!Files.isRegularFile(thumbnail)) {
                run(directory, ffmpeg, "-nostdin", "-y", "-v", "error", "-i", directory.resolve("360.mp4").toString(),
                        "-frames:v", "1", "-vf", fit(640, 360), "-threads", "1", thumbnail.toString());
            }
        }
        for (String name : List.of("360.mp4", "720.mp4", "1080.mp4", "auto-1.jpg", "auto-2.jpg", "auto-3.jpg")) {
            if (!Files.isRegularFile(directory.resolve(name)) || Files.size(directory.resolve(name)) == 0)
                throw new InvalidVideo("No se pudo generar el vídeo o sus miniaturas.");
        }
        return duration;
    }

    private static String fit(int width, int height) {
        return "scale=" + width + ":" + height + ":force_original_aspect_ratio=decrease:force_divisible_by=2,pad="
                + width + ":" + height + ":(ow-iw)/2:(oh-ih)/2,setsar=1";
    }

    private static String run(Path directory, String... args) throws IOException, InterruptedException {
        Path log = Files.createTempFile(directory, "process-", ".log");
        Process process = null;
        try {
            try {
                process = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            } catch (IOException ex) {
                throw new InvalidVideo("No se encontró " + (args[0].toLowerCase(Locale.ROOT).contains("probe") ? "FFprobe" : "FFmpeg")
                        + ". Revisa la instalación del servidor.");
            }
            if (!process.waitFor(15, TimeUnit.MINUTES)) throw new InvalidVideo("El procesamiento superó el tiempo permitido.");
            if (process.exitValue() != 0) throw new InvalidVideo("No se pudo procesar el vídeo. Revisa que no esté dañado y que su formato sea compatible.");
            try (var input = Files.newInputStream(log)) {
                return new String(input.readNBytes(65536), StandardCharsets.UTF_8);
            }
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            Files.deleteIfExists(log);
        }
    }

    static class InvalidVideo extends IOException {
        InvalidVideo(String message) { super(message); }
    }
}
