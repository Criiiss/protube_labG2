package com.tecnocampus.LS2.protube_back.search;

import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class SearchServiceTest {
    @Container static MongoDBContainer mongoDb = new MongoDBContainer("mongo:7");
    static SearchService search;

    @BeforeAll
    static void index() {
        search = new SearchService(new MongoTemplate(MongoClients.create(mongoDb.getConnectionString()), "test"));
        Instant now = Instant.now();
        search.replaceAll(List.of(
                SearchDocument.of("1", "Guitarra para principiantes", "Blue Sessions", List.of("música"), "acordes", 700, 500, now.minus(Duration.ofDays(2)), "/a.png"),
                SearchDocument.of("2", "Receta de paella", "Cocina Fácil", List.of("cocina", "guitarra"), "arroz", 200, 9000, now.minus(Duration.ofDays(400)), "/b.png"),
                SearchDocument.of("3", "Paella de marisco", "Cocina Fácil", List.of("cocina"), "con guitarra de fondo", 1500, 100, now, "/c.png"),
                SearchDocument.of("4", "Concierto de jazz", "Blue Sessions", List.of("jazz"), "", 4000, 3000, now.minus(Duration.ofDays(20)), "/d.png")),
                List.of(SearchPlaylist.of("10", "Lo mejor del jazz", "Ana", "para la noche", 3, now.minus(Duration.ofDays(5))),
                        SearchPlaylist.of("11", "Recetas rápidas", "Luis", "", 1, now)));
    }

    static SearchService.Result find(String q, String type, String date, String duration, String sort) {
        return search.search(new SearchService.Params(q, type, date, duration, sort, null, 0, 12));
    }
    static List<String> ids(SearchService.Result result) {
        return result.videos().stream().map(SearchService.VideoHit::id).toList();
    }

    @Test void titleMatchesRankAboveTagsAndDescription() {
        assertThat(ids(find("guitarra", "video", "any", "any", "relevance"))).containsExactly("1", "2", "3");
    }

    @Test void findsByChannelAndTagIgnoringAccents() {
        assertThat(ids(find("cocina facil", "video", "any", "any", "relevance"))).containsExactlyInAnyOrder("2", "3");
        assertThat(ids(find("musica", "video", "any", "any", "relevance"))).containsExactly("1");
    }

    @Test void correctsTyposAndCutWords() {
        SearchService.Result typo = find("guitrra", "video", "any", "any", "relevance");
        assertThat(typo.approximate()).isTrue();
        assertThat(typo.correctedQuery()).isEqualTo("guitarra");
        assertThat(ids(typo)).startsWith("1");
        assertThat(find("prinsipiantez", "video", "any", "any", "relevance").correctedQuery()).isEqualTo("principiantes");
        assertThat(find("guit", "video", "any", "any", "relevance").correctedQuery()).isEqualTo("guitarra");
        assertThat(find("guitarra", "video", "any", "any", "relevance").correctedQuery()).isNull();
    }

    @Test void emptyQueryReturnsEverythingWithoutErrors() {
        SearchService.Result result = find("   ", "all", "any", "any", "relevance");
        assertThat(result.videoTotal()).isEqualTo(4);
        assertThat(result.channels()).isEmpty();
        assertThat(result.playlists()).isEmpty();
        assertThat(find("", "channel", "any", "any", "relevance").channels())
                .extracting(SearchService.ChannelHit::name).containsExactly("Cocina Fácil", "Blue Sessions");
    }

    @Test void unknownWordsGiveNoResults() {
        SearchService.Result result = find("zzzzqqq", "all", "any", "any", "relevance");
        assertThat(result.videos()).isEmpty();
        assertThat(result.channels()).isEmpty();
        assertThat(result.playlists()).isEmpty();
        assertThat(result.correctedQuery()).isNull();
        assertThat(result.hasMore()).isFalse();
    }

    @Test void filtersByDateDurationAndSorts() {
        assertThat(ids(find("", "video", "week", "any", "date"))).containsExactly("3", "1");
        assertThat(ids(find("", "video", "any", "short", "relevance"))).containsExactly("2");
        assertThat(ids(find("", "video", "any", "medium", "relevance"))).containsExactly("1");
        assertThat(ids(find("", "video", "any", "long", "views"))).containsExactly("4", "3");
    }

    @Test void channelResultsAndChannelFilter() {
        SearchService.Result channels = find("blue", "channel", "any", "any", "relevance");
        assertThat(channels.videos()).isEmpty();
        assertThat(channels.channels()).singleElement().satisfies(c -> {
            assertThat(c.name()).isEqualTo("Blue Sessions");
            assertThat(c.videoCount()).isEqualTo(2);
            assertThat(c.viewCount()).isEqualTo(3500);
        });
        SearchService.Result typo = find("blu sesions", "channel", "any", "any", "relevance");
        assertThat(typo.channels()).hasSize(1);
        assertThat(typo.correctedQuery()).isEqualTo("blue sessions");
        SearchService.Result inChannel = search.search(new SearchService.Params("", "all", "any", "any", "views", "Blue Sessions", 0, 12));
        assertThat(ids(inChannel)).containsExactly("4", "1");
        assertThat(inChannel.channels()).isEmpty();
    }

    @Test void findsPlaylists() {
        SearchService.Result mixed = find("jazz", "all", "any", "any", "relevance");
        assertThat(ids(mixed)).containsExactly("4");
        assertThat(mixed.playlists()).singleElement().satisfies(p -> {
            assertThat(p.title()).isEqualTo("Lo mejor del jazz");
            assertThat(p.owner()).isEqualTo("Ana");
            assertThat(p.videoCount()).isEqualTo(3);
        });
        SearchService.Result all = find("", "playlist", "any", "any", "views");
        assertThat(all.videos()).isEmpty();
        assertThat(all.playlists()).extracting(SearchService.PlaylistHit::id).containsExactly("10", "11");
        assertThat(find("", "playlist", "any", "any", "date").playlists()).extracting(SearchService.PlaylistHit::id).containsExactly("11", "10");
        assertThat(find("recetas", "playlist", "any", "any", "relevance").playlistTotal()).isEqualTo(1);
    }

    @Test void paginates() {
        SearchService.Result first = search.search(new SearchService.Params("", "video", "any", "any", "views", null, 0, 3));
        SearchService.Result second = search.search(new SearchService.Params("", "video", "any", "any", "views", null, 1, 3));
        assertThat(ids(first)).containsExactly("2", "4", "1");
        assertThat(first.hasMore()).isTrue();
        assertThat(ids(second)).containsExactly("3");
        assertThat(second.hasMore()).isFalse();
        assertThat(search.search(new SearchService.Params("", "playlist", "any", "any", "views", null, 0, 1)).hasMore()).isTrue();
    }

    @Test void suggestsTitlesAndChannelsByWordPrefix() {
        assertThat(search.suggest("pae")).containsExactly("Receta de paella", "Paella de marisco");
        assertThat(search.suggest("Blu")).containsExactly("Blue Sessions");
        assertThat(search.suggest("  ")).isEmpty();
    }

    @Test void reindexingReplacesTheWholeIndex() {
        SearchService other = new SearchService(new MongoTemplate(MongoClients.create(mongoDb.getConnectionString()), "other"));
        other.replaceAll(List.of(SearchDocument.of("x", "Viejo", "C", List.of(), "", 10, 1, Instant.now(), "/x.png")), List.of());
        other.replaceAll(List.of(), List.of());
        assertThat(other.search(new SearchService.Params("", "all", "any", "any", "relevance", null, 0, 12)).videoTotal()).isZero();
    }
}
