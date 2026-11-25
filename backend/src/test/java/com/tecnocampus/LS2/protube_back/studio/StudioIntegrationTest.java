package com.tecnocampus.LS2.protube_back.studio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.auth.*;
import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "pro_tube.store.dir=target/test-media",
        "spring.datasource.url=jdbc:h2:mem:studio-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "protube.auth.dev-admin-enabled=false"
})
@AutoConfigureMockMvc
class StudioIntegrationTest {
    @TempDir static Path folder;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("protube.studio.directory", () -> folder.toString());
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired StudioService studio;
    @Autowired StudioVideos videos;
    @Autowired StudioStorage storage;
    @Autowired SpringDataUserRepository users;
    @Autowired AuthAccounts accounts;
    private Long owner, stranger;

    @BeforeEach void setup() {
        owner = createUser(25);
        stranger = createUser(16);
    }

    private Long createUser(int age) {
        String username = "studio_" + UUID.randomUUID().toString().substring(0, 12);
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var user = users.saveAndFlush(new UserJpaEntity(null, username, username + "@example.test", "unused", "LOCAL", null, now, now));
        var account = new AuthAccount();
        account.userId = user.getId();
        account.birthDate = LocalDate.now().minusYears(age);
        account.termsAcceptedAt = now;
        account.emailVerified = true;
        accounts.save(account);
        return user.getId();
    }

    private MockHttpSession session(Long id) {
        var session = new MockHttpSession();
        session.setAttribute("authUser", id);
        session.setAttribute("authVersion", 0);
        session.setAttribute("authDeadline", Instant.now().plusSeconds(300));
        return session;
    }

    private VideoMetadata meta(VideoMetadata.Visibility visibility, Instant time, boolean draft, boolean adult, String thumbnail) {
        return new VideoMetadata("Studio test", "Description", List.of("test", "test"), "music", "es",
                adult, false, visibility, time, draft, thumbnail);
    }

    private StudioService.Start input(byte[] bytes, VideoMetadata metadata) throws Exception {
        return new StudioService.Start("clip.mp4", bytes.length,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)), metadata);
    }

    @Test void uploadResumesAtAcknowledgedOffsetAndDeniesOtherOwners() throws Exception {
        var bytes = new byte[]{1, 2, 3, 4};
        var input = input(bytes, meta(VideoMetadata.Visibility.PRIVATE, null, false, false, null));
        var body = json.writeValueAsString(input);
        mvc.perform(post("/api/studio/videos").contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/studio/videos").header("X-Protube-Request", "1").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
        var response = mvc.perform(post("/api/studio/videos").session(session(owner)).header("X-Protube-Request", "1")
                .contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();
        String id = json.readTree(response.getResponse().getContentAsString()).get("id").asText();
        String path = "/api/studio/videos/" + id;
        mvc.perform(put(path + "/chunk").param("offset", "0").session(session(owner)).header("X-Protube-Request", "1")
                .contentType("application/octet-stream").content(new byte[]{1, 2}))
                .andExpect(status().isOk()).andExpect(jsonPath("$.receivedBytes").value(2));
        assertEquals(2, studio.start(owner, input).receivedBytes());
        mvc.perform(get(path).session(session(owner))).andExpect(status().isOk()).andExpect(jsonPath("$.receivedBytes").value(2));
        mvc.perform(get("/api/studio/videos").session(session(owner))).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(patch(path).session(session(stranger)).header("X-Protube-Request", "1")
                .contentType("application/json").content(json.writeValueAsString(input.metadata())))
                .andExpect(status().isForbidden());
        assertEquals(409, assertThrows(AuthFailure.class, () -> studio.chunk(owner, id, 0, bytes)).status);
        assertEquals(409, assertThrows(AuthFailure.class, () -> studio.finish(owner, id)).status);
        studio.chunk(owner, id, 2, new byte[]{3, 4});
        assertArrayEquals(bytes, Files.readAllBytes(storage.source(id)));
        mvc.perform(delete(path).session(session(owner)).header("X-Protube-Request", "1")).andExpect(status().isNoContent());
        assertFalse(Files.exists(storage.source(id)));
        assertTrue(studio.list(owner).isEmpty());
    }

    @Test void corruptOrDifferentUploadBecomesErrorAndNeverAppearsInCatalog() throws Exception {
        var item = studio.start(owner, input(new byte[]{1, 2, 3}, meta(VideoMetadata.Visibility.PUBLIC, null, false, false, null)));
        studio.chunk(owner, item.id(), 0, new byte[]{4, 5, 6});
        mvc.perform(post("/api/studio/videos/" + item.id() + "/finish").session(session(owner)).header("X-Protube-Request", "1"))
                .andExpect(status().isOk());
        var deadline = Instant.now().plusSeconds(10);
        StudioService.Item result;
        do {
            result = studio.get(owner, item.id());
            if (result.status() == StudioVideo.Status.ERROR) break;
            Thread.sleep(100);
        } while (Instant.now().isBefore(deadline));
        assertEquals(StudioVideo.Status.ERROR, result.status());
        assertTrue(result.failure().contains("no coincide"));
        assertTrue(studio.catalog(owner).stream().noneMatch(entry -> entry.id().equals(item.id())));
        mvc.perform(get("/api/studio/play/" + item.id() + "/360.mp4").session(session(owner))).andExpect(status().isNotFound());
    }

    @Test void publicationAndPlaybackRespectScheduleDraftPrivacyAndAge() throws Exception {
        byte[] bytes = {4};
        var item = studio.start(owner, input(bytes, meta(VideoMetadata.Visibility.PUBLIC, Instant.now().plusSeconds(3600), false, false, null)));
        String id = item.id();
        var video = videos.findById(id).orElseThrow();
        video.status = StudioVideo.Status.READY;
        video.duration = 64;
        videos.saveAndFlush(video);
        Files.createDirectories(storage.directory(id));
        Files.write(storage.directory(id).resolve("360.mp4"), new byte[]{1, 2, 3, 4});
        assertFalse(studio.catalog(null).stream().anyMatch(entry -> entry.id().equals(id)));
        assertThrows(AuthFailure.class, () -> studio.playback(stranger, id));
        assertEquals(id, studio.playback(owner, id).id());
        studio.edit(owner, id, meta(VideoMetadata.Visibility.PUBLIC, null, true, false, null));
        assertThrows(AuthFailure.class, () -> studio.playback(null, id));
        studio.edit(owner, id, meta(VideoMetadata.Visibility.PRIVATE, null, false, false, null));
        assertThrows(AuthFailure.class, () -> studio.playback(stranger, id));
        studio.edit(owner, id, meta(VideoMetadata.Visibility.UNLISTED, null, false, false, null));
        assertEquals(id, studio.playback(null, id).id());
        assertFalse(studio.catalog(null).stream().anyMatch(entry -> entry.id().equals(id)));
        studio.edit(owner, id, meta(VideoMetadata.Visibility.PUBLIC, Instant.now().minusSeconds(1), false, true, null));
        assertThrows(AuthFailure.class, () -> studio.playback(stranger, id));
        assertEquals(id, studio.playback(createUser(30), id).id());
        mvc.perform(get("/api/studio/play/" + id).session(session(owner))).andExpect(jsonPath("$.commentsAllowed").value(false));
        mvc.perform(get("/api/studio/play/" + id + "/360.mp4").session(session(owner)).header("Range", "bytes=0-1"))
                .andExpect(status().isPartialContent()).andExpect(content().bytes(new byte[]{1, 2}));
        studio.edit(owner, id, meta(VideoMetadata.Visibility.PUBLIC, null, false, false, null));
        assertTrue(studio.catalog(null).stream().anyMatch(entry -> entry.id().equals(id) && entry.duration().equals("1:04")));
        mvc.perform(get("/api/catalog")).andExpect(status().isOk());
    }

    @Test void validatesVideoLimitsAndCustomThumbnailBeforeSelection() throws Exception {
        var metadata = meta(VideoMetadata.Visibility.PRIVATE, null, false, false, null);
        var request = input(new byte[]{9}, metadata);
        assertThrows(AuthFailure.class, () -> studio.start(owner, new StudioService.Start("bad.avi", 1, request.checksum(), metadata)));
        assertThrows(AuthFailure.class, () -> studio.start(owner, new StudioService.Start("huge.mov", 200000000, request.checksum(), metadata)));
        assertThrows(AuthFailure.class, () -> studio.start(owner, new StudioService.Start("bad.mp4", 1, "invalid", metadata)));
        assertThrows(AuthFailure.class, () -> studio.start(owner, input(new byte[]{9},
                meta(VideoMetadata.Visibility.PRIVATE, Instant.now().plusSeconds(60), false, false, null))));
        var id = studio.start(owner, request).id();
        assertThrows(AuthFailure.class, () -> studio.edit(owner, id, meta(VideoMetadata.Visibility.PRIVATE, null, false, false, "custom")));
        assertThrows(AuthFailure.class, () -> studio.thumbnail(owner, id, image(640, 400)));
        mvc.perform(put("/api/studio/videos/" + id + "/thumbnail").session(session(owner)).header("X-Protube-Request", "1")
                .contentType("application/octet-stream").content(image(640, 360))).andExpect(status().isOk());
        assertEquals("custom", studio.edit(owner, id, meta(VideoMetadata.Visibility.PRIVATE, null, false, false, "custom")).metadata().thumbnail());
        mvc.perform(get("/api/studio/videos/" + id + "/thumbnail/custom.jpg").session(session(owner)))
                .andExpect(content().contentType("image/jpeg"));
        assertEquals(403, assertThrows(AuthFailure.class, () -> studio.preview(stranger, id, "custom.jpg")).status);
        mvc.perform(get("/api/studio/limits")).andExpect(jsonPath("$.maxSeconds").value(1200));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mp4", "mov", "webm"})
    void realEncoderProducesThreePlayableQualitiesAndThumbnailsWhenAvailable(String extension) throws Exception {
        String ffmpeg = System.getenv("PROTUBE_STUDIO_FFMPEG"), ffprobe = System.getenv("PROTUBE_STUDIO_FFPROBE");
        assumeTrue(ffmpeg != null && ffprobe != null, "Set paths to FFmpeg and FFprobe to run the codec smoke test.");
        Path fixture = folder.resolve("fixture." + extension);
        var encoder = new ProcessBuilder(ffmpeg, "-y", "-v", "error", "-f", "lavfi", "-i", "color=c=blue:s=320x180:r=12:d=2",
                "-c:v", extension.equals("webm") ? "libvpx-vp9" : "libx264", "-pix_fmt", "yuv420p", fixture.toString()).redirectErrorStream(true).start();
        assertTrue(encoder.waitFor(30, TimeUnit.SECONDS));
        assertEquals(0, encoder.exitValue(), new String(encoder.getInputStream().readAllBytes()));
        byte[] bytes = Files.readAllBytes(fixture);
        var source = input(bytes, meta(VideoMetadata.Visibility.PUBLIC, null, false, false, null));
        var item = studio.start(owner, new StudioService.Start("clip." + extension, source.bytes(), source.checksum(), source.metadata()));
        studio.chunk(owner, item.id(), 0, bytes);
        var durationCheck = new VideoProcessor(storage, json, ffmpeg, ffprobe, 1);
        assertThrows(VideoProcessor.InvalidVideo.class, () -> durationCheck.process(videos.findById(item.id()).orElseThrow()));
        studio.finish(owner, item.id());
        var deadline = Instant.now().plusSeconds(20);
        StudioService.Item result;
        do {
            result = studio.get(owner, item.id());
            if (Set.of(StudioVideo.Status.READY, StudioVideo.Status.ERROR).contains(result.status())) break;
            Thread.sleep(100);
        } while (Instant.now().isBefore(deadline));
        assertEquals(StudioVideo.Status.READY, result.status(), result.failure());
        assertEquals(2.0, result.duration(), 0.2);
        assertTrue(studio.catalog(null).stream().anyMatch(entry -> entry.id().equals(item.id())));
        for (String height : List.of("360", "720", "1080")) {
            var probe = new ProcessBuilder(ffprobe, "-v", "error", "-select_streams", "v:0", "-show_entries",
                    "stream=height", "-of", "csv=p=0", storage.directory(item.id()).resolve(height + ".mp4").toString()).start();
            assertTrue(probe.waitFor(15, TimeUnit.SECONDS));
            assertEquals(height, new String(probe.getInputStream().readAllBytes()).strip());
        }
        for (int i = 1; i <= 3; i++) {
            var thumbnail = ImageIO.read(storage.directory(item.id()).resolve("auto-" + i + ".jpg").toFile());
            assertEquals(640, thumbnail.getWidth());
            assertEquals(360, thumbnail.getHeight());
        }
    }

    private byte[] image(int width, int height) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }
}
