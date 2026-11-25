package com.tecnocampus.LS2.protube_back.search;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchUnitTest {
    @Test void paramsFallBackToSafeDefaults() {
        SearchService.Params p = new SearchService.Params("  x".repeat(60), "podcast", "decade", "huge", "random", " ", -3, 500);
        assertThat(p.q()).hasSize(100);
        assertThat(List.of(p.type(), p.date(), p.duration(), p.sort())).containsExactly("all", "any", "any", "relevance");
        assertThat(p.channel()).isNull();
        assertThat(p.page()).isZero();
        assertThat(p.size()).isEqualTo(48);
        assertThat(new SearchService.Params(null, "playlist", "week", "long", "date", "C", 2, 0).q()).isEmpty();
    }

    @Test void normalizesAccentsCaseAndPunctuation() {
        assertThat(SearchDocument.normalize("¡Música  Épica!")).isEqualTo("musica epica");
        assertThat(SearchDocument.normalize(null)).isEmpty();
        SearchDocument doc = SearchDocument.of("1", "La Música", "Yo", null, null, 1, 1, Instant.now(), "/t.png");
        assertThat(doc.tags()).isEmpty();
        assertThat(doc.terms()).containsExactly("musica"); // words shorter than 3 letters are not useful to correct
    }

    @Test void correctsWithTheClosestKnownWord() {
        Set<String> words = Set.of("guitarra", "principiantes", "paella", "jazz", "paz");
        assertThat(SearchService.closest("guitrra", words)).isEqualTo("guitarra");     // 1 missing letter
        assertThat(SearchService.closest("gutiarra", words)).isEqualTo("guitarra");    // 2 letters swapped = 1 typo
        assertThat(SearchService.closest("prinsipiantez", words)).isEqualTo("principiantes"); // 2 typos in a long word
        assertThat(SearchService.closest("paela", words)).isEqualTo("paella");
        assertThat(SearchService.closest("pae", words)).isEqualTo("paella");          // completion
        assertThat(SearchService.closest("jaz", words)).isEqualTo("jazz");
        assertThat(SearchService.closest("piano", words)).isEqualTo("piano");         // nothing close: unchanged
        assertThat(SearchService.closest("pz", words)).isEqualTo("pz");
        assertThat(SearchService.distance("abc", "abc")).isZero();
        assertThat(SearchService.distance("", "abc")).isEqualTo(3);
    }

    @Test void formatsHits() {
        Instant now = Instant.now();
        SearchService.VideoHit hit = SearchService.hit(SearchDocument.of("7", "T", "C", List.of("a"), "", 3725, 42, now, "/t.png"));
        assertThat(hit.duration()).isEqualTo("1:02:05");
        assertThat(hit.views()).isEqualTo("42 visualizaciones");
        assertThat(SearchService.hit(SearchDocument.of("8", "T", "C", List.of(), "", 65, 0, now, "/t.png")).duration()).isEqualTo("1:05");
        assertThat(List.of(0, 1, 5, 45, 90, 400, 1000).stream().map(d -> SearchService.ago(now.minus(Duration.ofDays(d)))).toList())
                .containsExactly("hoy", "hace 1 día", "hace 5 días", "hace 1 mes", "hace 3 meses", "hace 1 año", "hace 2 años");
    }

    @Test void controllerPassesParamsAndReports503WhenMongoIsDown() throws Exception {
        SearchService service = mock(SearchService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SearchController(service)).build();
        when(service.search(any())).thenReturn(new SearchService.Result("paella", List.of(), 0, List.of(), 0, List.of(), 0, 1, false, false, null));
        when(service.suggest("pa")).thenReturn(List.of("Paella"));

        mvc.perform(get("/api/search").param("q", "paella").param("type", "video").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.query").value("paella"));
        verify(service).search(new SearchService.Params("paella", "video", "any", "any", "relevance", null, 1, 12));
        mvc.perform(get("/api/search/suggest").param("q", "pa")).andExpect(jsonPath("$[0]").value("Paella"));

        when(service.search(any())).thenThrow(new DataAccessResourceFailureException("down"));
        mvc.perform(get("/api/search")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test void indexerSwallowsMongoFailures() {
        SearchService service = mock(SearchService.class);
        doThrow(new DataAccessResourceFailureException("down")).when(service).replaceAll(any(), any());
        new SearchIndexer(mock(JdbcTemplate.class), service).reindex(); // must not throw
        verify(service).replaceAll(List.of(), List.of());
    }
}
