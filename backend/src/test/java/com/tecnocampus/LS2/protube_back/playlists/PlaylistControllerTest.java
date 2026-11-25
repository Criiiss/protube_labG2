package com.tecnocampus.LS2.protube_back.playlists;

import com.tecnocampus.LS2.protube_back.auth.AuthFailure;
import com.tecnocampus.LS2.protube_back.auth.AuthService;
import com.tecnocampus.LS2.protube_back.search.SearchIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlaylistControllerTest {
    PlaylistService playlists = mock(PlaylistService.class);
    AuthService auth = mock(AuthService.class);
    SearchIndexer indexer = mock(SearchIndexer.class);
    MockMvc mvc;
    MockHttpSession session;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PlaylistController(playlists, auth, indexer)).build();
        when(auth.version(7L)).thenReturn(0);
        session = new MockHttpSession();
        session.setAttribute("authUser", 7L);
        session.setAttribute("authVersion", 0);
        session.setAttribute("authDeadline", Instant.now().plusSeconds(600));
    }

    @Test void requiresLoginToChangePlaylists() throws Exception {
        mvc.perform(get("/api/playlists/mine")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Inicia sesión para continuar."));
        mvc.perform(post("/api/playlists").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(playlists);
    }

    @Test void managesPlaylistsAndReindexesAfterEachChange() throws Exception {
        when(playlists.mine(7L, 5L)).thenReturn(List.of(new PlaylistService.Summary(1, "Fav", "PUBLIC", 2, true)));
        when(playlists.create(eq(7L), any())).thenReturn(new PlaylistService.Summary(2, "Nueva", "PRIVATE", 0, false));

        mvc.perform(get("/api/playlists/mine").param("videoId", "5").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].hasVideo").value(true));
        mvc.perform(get("/api/playlists/mine").param("videoId", "studio-id").session(session)).andExpect(status().isOk());
        verify(playlists).mine(7L, null);
        mvc.perform(post("/api/playlists").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Nueva\",\"visibility\":\"PRIVATE\"}")).andExpect(jsonPath("$.id").value(2));
        mvc.perform(post("/api/playlists/2/videos").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"videoId\":\"5\"}"))
                .andExpect(jsonPath("$.ok").value(true));
        mvc.perform(delete("/api/playlists/2/videos/5").session(session)).andExpect(status().isOk());
        mvc.perform(delete("/api/playlists/2").session(session)).andExpect(status().isOk());

        verify(playlists).create(7L, new PlaylistService.NewPlaylist("Nueva", null, "PRIVATE"));
        verify(playlists).addVideo(7L, 2L, "5");
        verify(playlists).removeVideo(7L, 2L, "5");
        verify(playlists).delete(7L, 2L);
        verify(indexer, times(4)).reindex();
    }

    @Test void publicPlaylistsCanBeSeenWithoutLoginAndErrorsKeepTheirStatus() throws Exception {
        when(playlists.get(3L, null)).thenReturn(new PlaylistService.Detail(3, "Pública", null, "PUBLIC", "ana", false, List.of()));
        mvc.perform(get("/api/playlists/3")).andExpect(status().isOk()).andExpect(jsonPath("$.owner").value("ana"));
        when(playlists.get(4L, 7L)).thenThrow(new AuthFailure(404, "Esta playlist no existe o es privada."));
        mvc.perform(get("/api/playlists/4").session(session)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Esta playlist no existe o es privada."));
    }
}
