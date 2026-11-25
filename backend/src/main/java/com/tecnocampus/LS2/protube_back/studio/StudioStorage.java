package com.tecnocampus.LS2.protube_back.studio;

import com.tecnocampus.LS2.protube_back.auth.AuthFailure;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import javax.imageio.ImageIO;

@Component
public class StudioStorage {
    private final Path root;

    public StudioStorage(@Value("${pro_tube.store.dir}") String media,
                         @Value("${protube.studio.directory:}") String configured) {
        Path publicRoot = Path.of(media).toAbsolutePath().normalize();
        root = configured.isBlank() ? publicRoot.resolveSibling(publicRoot.getFileName() + ".studio")
                : Path.of(configured).toAbsolutePath().normalize();
        if (root.startsWith(publicRoot)) throw new IllegalArgumentException("Studio necesita una carpeta fuera de /media.");
    }

    Path directory(String id) {
        return root.resolve(id);
    }

    Path source(String id) {
        return directory(id).resolve("source.upload");
    }

    void append(String id, long offset, byte[] bytes) throws IOException {
        Files.createDirectories(directory(id));
        try (var file = new RandomAccessFile(source(id).toFile(), "rw")) {
            if (file.length() < offset) throw new IOException("Incomplete source file");
            file.setLength(offset);
            file.seek(offset);
            file.write(bytes);
            file.getFD().sync();
        }
    }

    String checksum(Path file) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var input = new DigestInputStream(Files.newInputStream(file), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    void thumbnail(String id, byte[] bytes) throws IOException {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw badImage();
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (!reader.getFormatName().matches("(?i)jpeg|png") || width < 640 || width > 3840
                        || height > 2160 || width * 9 != height * 16) throw badImage();
                var image = reader.read(0);
                var rgb = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics = rgb.createGraphics();
                try { graphics.drawImage(image, 0, 0, null); } finally { graphics.dispose(); }
                Files.createDirectories(directory(id));
                Path temporary = Files.createTempFile(directory(id), "thumbnail-", ".jpg");
                try {
                    ImageIO.write(rgb, "jpeg", temporary.toFile());
                    Files.move(temporary, directory(id).resolve("custom.jpg"), StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temporary); }
            } finally { reader.dispose(); }
        } catch (javax.imageio.IIOException ex) { throw badImage(); }
    }

    private static AuthFailure badImage() {
        return new AuthFailure(400, "Usa una miniatura JPG o PNG de proporción 16:9, entre 640×360 y 3840×2160.");
    }

    void remove(String id) throws IOException {
        if (!Files.isDirectory(directory(id))) return;
        try (var entries = Files.list(directory(id))) {
            for (var file : entries.toList()) Files.deleteIfExists(file);
        }
        Files.deleteIfExists(directory(id));
    }
}
