package com.tecnocampus.LS2.protube_back.studio;

import com.tecnocampus.LS2.protube_back.auth.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/studio")
public class StudioController {
    private final StudioService studio;
    private final AuthService auth;

    public StudioController(StudioService studio, AuthService auth) { this.studio = studio; this.auth = auth; }

    @GetMapping("/limits") StudioService.Limits limits() { return studio.limits(); }
    @GetMapping("/videos") List<StudioService.Item> list(HttpServletRequest request) { return studio.list(owner(request)); }
    @PostMapping("/videos") StudioService.Item start(HttpServletRequest request, @RequestBody StudioService.Start input) { return studio.start(owner(request), input); }
    @GetMapping("/videos/{id}") StudioService.Item get(HttpServletRequest request, @PathVariable String id) { return studio.get(owner(request), id); }
    @PatchMapping("/videos/{id}") StudioService.Item edit(HttpServletRequest request, @PathVariable String id, @RequestBody VideoMetadata metadata) {
        return studio.edit(owner(request), id, metadata);
    }
    @PutMapping(value = "/videos/{id}/chunk", consumes = "application/octet-stream")
    StudioService.Item chunk(HttpServletRequest request, @PathVariable String id, @RequestParam long offset) throws IOException {
        Long owner = owner(request);
        return studio.chunk(owner, id, offset, request.getInputStream().readNBytes(StudioService.CHUNK_SIZE + 1));
    }
    @PostMapping("/videos/{id}/finish") StudioService.Item finish(HttpServletRequest request, @PathVariable String id) {
        return studio.finish(owner(request), id);
    }
    @DeleteMapping("/videos/{id}") ResponseEntity<Void> cancel(HttpServletRequest request, @PathVariable String id) throws IOException {
        studio.cancel(owner(request), id); return ResponseEntity.noContent().build();
    }
    @PutMapping(value = "/videos/{id}/thumbnail", consumes = "application/octet-stream")
    StudioService.Item thumbnail(HttpServletRequest request, @PathVariable String id) throws IOException {
        Long owner = owner(request);
        return studio.thumbnail(owner, id, request.getInputStream().readNBytes(StudioService.IMAGE_SIZE + 1));
    }
    @GetMapping("/videos/{id}/thumbnail/{name}") ResponseEntity<Resource> preview(HttpServletRequest request, @PathVariable String id, @PathVariable String name) {
        return file(studio.preview(owner(request), id, name));
    }
    @GetMapping("/play/{id}") StudioService.Playback play(HttpServletRequest request, @PathVariable String id) {
        return studio.playback(viewer(request, auth), id);
    }
    @GetMapping("/play/{id}/{name}") ResponseEntity<Resource> media(HttpServletRequest request, @PathVariable String id, @PathVariable String name) {
        return file(studio.publicFile(viewer(request, auth), id, name));
    }

    private Long owner(HttpServletRequest request) { return AuthController.requireUser(request, auth); }

    public static Long viewer(HttpServletRequest request, AuthService auth) {
        try { return AuthController.requireUser(request, auth); }
        catch (AuthFailure ex) { if (ex.status != 401) throw ex; return null; }
    }

    private ResponseEntity<Resource> file(Resource resource) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(resource.getFilename().endsWith(".jpg") ? MediaType.IMAGE_JPEG : MediaType.valueOf("video/mp4"))
                .body(resource);
    }

    @ExceptionHandler(AuthFailure.class) ResponseEntity<?> failure(AuthFailure failure) {
        return ResponseEntity.status(failure.status).body(Map.of("message", failure.getMessage()));
    }
    @ExceptionHandler(IOException.class) ResponseEntity<?> ioError() {
        return ResponseEntity.internalServerError().body(Map.of("message", "No se pudo guardar el archivo. Comprueba la conexión y el espacio disponible."));
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class) ResponseEntity<?> invalid() {
        return ResponseEntity.badRequest().body(Map.of("message", "Los datos de vídeo no tienen un formato válido."));
    }
}
