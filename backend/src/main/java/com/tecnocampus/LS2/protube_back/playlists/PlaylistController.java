package com.tecnocampus.LS2.protube_back.playlists;

import com.tecnocampus.LS2.protube_back.auth.AuthController;
import com.tecnocampus.LS2.protube_back.auth.AuthFailure;
import com.tecnocampus.LS2.protube_back.auth.AuthService;
import com.tecnocampus.LS2.protube_back.search.SearchIndexer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
    public record AddVideo(String videoId) {}

    private final PlaylistService playlists;
    private final AuthService auth;
    private final SearchIndexer searchIndexer;

    public PlaylistController(PlaylistService playlists, AuthService auth, SearchIndexer searchIndexer) {
        this.playlists = playlists; this.auth = auth; this.searchIndexer = searchIndexer;
    }

    @GetMapping("/mine")
    public List<PlaylistService.Summary> mine(@RequestParam(required = false) String videoId, HttpServletRequest request) {
        Long id = null;
        try { id = videoId == null ? null : Long.valueOf(videoId); } catch (NumberFormatException ignored) { /* Studio id */ }
        return playlists.mine(user(request), id);
    }

    @PostMapping
    public PlaylistService.Summary create(@RequestBody PlaylistService.NewPlaylist body, HttpServletRequest request) {
        var created = playlists.create(user(request), body);
        searchIndexer.reindex();
        return created;
    }

    @GetMapping("/{id}")
    public PlaylistService.Detail get(@PathVariable long id, HttpServletRequest request) {
        Long viewer = null;
        try { viewer = user(request); } catch (AuthFailure anonymous) { /* public playlists are visible without login */ }
        return playlists.get(id, viewer);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@PathVariable long id, HttpServletRequest request) {
        playlists.delete(user(request), id);
        searchIndexer.reindex();
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/videos")
    public Map<String, Boolean> add(@PathVariable long id, @RequestBody AddVideo body, HttpServletRequest request) {
        playlists.addVideo(user(request), id, body == null ? null : body.videoId());
        searchIndexer.reindex();
        return Map.of("ok", true);
    }

    @DeleteMapping("/{id}/videos/{videoId}")
    public Map<String, Boolean> remove(@PathVariable long id, @PathVariable String videoId, HttpServletRequest request) {
        playlists.removeVideo(user(request), id, videoId);
        searchIndexer.reindex();
        return Map.of("ok", true);
    }

    @ExceptionHandler(AuthFailure.class)
    ResponseEntity<Map<String, String>> failure(AuthFailure ex) {
        return ResponseEntity.status(ex.status).body(Map.of("message", ex.getMessage()));
    }

    private Long user(HttpServletRequest request) { return AuthController.requireUser(request, auth); }
}
